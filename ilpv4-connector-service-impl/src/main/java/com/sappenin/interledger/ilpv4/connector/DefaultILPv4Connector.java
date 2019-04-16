package com.sappenin.interledger.ilpv4.connector;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.events.IlpNodeEvent;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ModifiableConnectorSettings;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ModifiableAccountSettings;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketMapper;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpRequest;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * <p>A default implementation of an {@link ILPv4Connector}.</p>
 *
 * <h1>Accounts and Links</h1>
 * <p>A Connector Account contains a Link, and cannot be created until the Link is created. For some
 * link/account combinations, this means that a Link must first connect, and then emits a {@link LinkConnectedEvent},
 * the Connector will respond to by constructing and beinging to manager an Account. In other cases, an Account is
 * pre-configured in this node, and thus triggers the creation of a link which connects to a remote server (the
 * LinkConnectedEvent process is the same in this case).
 * </p>
 *
 * <h1>^^ Account Manager ^^</h1>
 * <p>Connector accounts are managed by the {@link AccountManager}. Accounts can be added to the AccountManager
 * indirectly in response to a particular Link emitting a {@link LinkConnectedEvent}. This process can be initiated
 * during Connector startup (by configuring accounts in a properties file, database, or other mechanism); or at runtime
 * via a configuration mechanism (e.g., Admin API). Additionally, depending on the Link type, incoming connections can
 * be made to the Connector, which will trigger link creation and connection, and thus account tracking via the same
 * event mechanism.
 * </p>
 *
 * <h1>^^ Link Provider ^^</h1>
 * <p>Some link types are created dynamically in response to various events using a {@link LinkFactoryProvider}.
 * To be used by this connector, every link must have a corresponding factory class, which must be registered with the
 * factory provider.</p>
 *
 * <h1>^^ Link Factory ^^</h1>
 * <p>This is the primary way that links are be constructed. Each link type should have an eligible factory.</p>
 *
 * <h1>^^ Account Tracking ^^</h1>
 * When an account is constructed in response to a link connecting, the account is added to the {@link AccountManager}
 * for tracking. This makes the account eligible for balance tracking, packet-switching, and route-propagation via the
 * CCP.
 *
 * <h2>^^ Server Account Tracking ^^</h2>
 * Some links support multiple accounts, such as a BTP WebSocket Server. For each connection the server accepts, a new
 * link will be instantiated and its `connect` method called. This will trigger a {@link LinkConnectedEvent} that the
 * connector will listen to, and instruct the AccountManager to begin tracking this account. If the link disconnects,
 * this Connector will be listening, and instruct the AccountManager to stop tracking the account.
 *
 * <h2>^^ Client Account Tracking ^^</h2>
 * Other links initiate an outbound connection to a remote server. For example, a BTP Client link will do this, and it
 * may not receive its account information until _after_ the link has connected. Thus, this Connector listens for the
 * same {@link LinkConnectedEvent} events as above, and tracks or untracks each Account based upon what the
 * corresponding Link does.
 */
public class DefaultILPv4Connector implements ILPv4Connector {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final EventBus eventBus;

  // TODO: Consider placing the LinkManager back inside of the AccountManager.
  private final AccountManager accountManager;
  private final LinkManager linkManager;

  private final InternalRoutingService internalRoutingService;
  private final ExternalRoutingService externalRoutingService;
  private final BalanceTracker balanceTracker;

  // Handles all packet addresses.
  private final ILPv4PacketSwitch ilpPacketSwitch;

  /**
   * Required-args Constructor (minus an EventBus).
   */
  public DefaultILPv4Connector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final LinkManager linkManager,
    final InternalRoutingService internalRoutingService,
    final ExternalRoutingService externalRoutingService,
    final ILPv4PacketSwitch ilpPacketSwitch,
    final BalanceTracker balanceTracker
  ) {
    this(
      connectorSettingsSupplier,
      accountManager,
      linkManager,
      internalRoutingService,
      externalRoutingService,
      ilpPacketSwitch,
      balanceTracker,
      new EventBus()
    );
  }

  public DefaultILPv4Connector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final LinkManager linkManager,
    final InternalRoutingService internalRoutingService, final ExternalRoutingService externalRoutingService,
    final ILPv4PacketSwitch ilpPacketSwitch,
    final BalanceTracker balanceTracker,
    final EventBus eventBus
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.internalRoutingService = Objects.requireNonNull(internalRoutingService);
    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
    this.ilpPacketSwitch = Objects.requireNonNull(ilpPacketSwitch);
    this.eventBus = Objects.requireNonNull(eventBus);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
    eventBus.register(this);
  }

  /**
   * <p>Initialize the connector after constructing it.</p>
   */
  @PostConstruct
  private final void init() {


    ////////////////////////////////
    // ^^ Pre-Configured Accounts ^^
    ////////////////////////////////
    // If an operator address is specified, then we use that. Otherwise, attempt to use IL-DCP.
    if (connectorSettingsSupplier.get().getOperatorAddress().isPresent()) {
      this.configureAccounts();
    } else {
      // ^^ IL-DCP ^^
      this.configureAccountsUsingIldcp();
    }

    this.getExternalRoutingService().start();
  }

  @PreDestroy
  public void shutdown() {
    // Shutdown all links...This will emit LinkDisconnected events that will be handled below...
    this.accountManager.getAllAccounts().forEach(account -> {
      account.getLink().disconnect();
    });
  }

  @Override
  public ConnectorSettings getConnectorSettings() {
    return this.connectorSettingsSupplier.get();
  }

  @Override
  public AccountManager getAccountManager() {
    return this.accountManager;
  }

  @Override
  public LinkManager getLinkManager() {
    return this.linkManager;
  }

  @Override
  public ExternalRoutingService getExternalRoutingService() {
    return this.externalRoutingService;
  }

  @Override
  public InternalRoutingService getInternalRoutingService() {
    return this.internalRoutingService;
  }

  @Override
  public BalanceTracker getBalanceTracker() {
    return this.balanceTracker;
  }

  @Override
  public ILPv4PacketSwitch getIlpPacketSwitch() {
    return this.ilpPacketSwitch;
  }

  @Override
  public Optional<InterledgerAddress> getNodeIlpAddress() {
    return this.connectorSettingsSupplier.get().getOperatorAddress();
  }

  /**
   * Handle an event of type {@link IlpNodeEvent}.
   *
   * @param event the event to respond to.
   *
   * @deprecated TODO Determine if this method is necessary. It may be desirable to instead use the EventBus.
   */
  @EventListener
  @Deprecated
  public void onApplicationEvent(final IlpNodeEvent event) {

  }

  private void configureAccountsUsingIldcp() {
    /////////////////////////////////
    // ^^ Parent Account ^^
    /////////////////////////////////
    // If this Connector is starting in `child` mode, it will not have an operator address. We need to find the first
    // account of type `parent` and use IL-DCP to get the operating account for this Connector.
    CompletableFuture.supplyAsync(() -> {
      // If there's no Operator Address, use IL-DCP to try to get one. Only try this once. If this fails, then the
      // Connector shoudl not startup.
      final AccountSettings firstParentAccountSettings = this.findParentAccountSettings();
      // Throws an exception if IL-DCP doesn't work.
      return initializeParentAccount(firstParentAccountSettings);
    })
      // Only after we have obtained a parent-account should we attempt to initialize the other accounts...
      .thenAccept(firstParentAccount -> {
        /////////////////////////////////
        // ^^ Preconfigurated Accounts ^^
        /////////////////////////////////
        configureAccounts(firstParentAccount);
        logger.info(
          "IL-DCP Succeeded! Operator Address: `{}`", connectorSettingsSupplier.get().getOperatorAddress().get()
        );
      })
      .exceptionally(error -> {
        logger.error("Unable to initialize this Connector using IL-DCP: " + error.getMessage(), error);
        return null;
      });
  }


  /**
   * If there's a parent account, then assume it has already been configured via IL-DCP. Otherwise, assume no parent
   * account, and configure all accounts.
   */
  private void configureAccounts() {
    this.connectorSettingsSupplier.get().getAccountSettings().stream()
      // Connect all links, regardless of type. Server-links won't be defined in here, and if there's a peer link
      // that this connector is a client of, then it will be connected here, if appropriate. Links that require an
      // incoming and outgoing connection will not emit a LinkConnectedEvent until both connections are connected.
      .forEach(accountManager::createAccount);
  }

  /**
   * If there's a parent account, then assume it has already been configured via IL-DCP. Otherwise, assume no parent
   * account, and configure all accounts.
   *
   * @param firstParentAccount
   */
  private void configureAccounts(final Account firstParentAccount) {
    this.connectorSettingsSupplier.get().getAccountSettings().stream()
      // Don't use the parent account that was chosen above...
      .filter(accountSettings -> accountSettings.getAccountId().equals(firstParentAccount.getId()) == false)
      // Connect all links, regardless of type. Server-links won't be defined in here, and if there's a peer link
      // that this connector is a client of, then it will be connected here, if appropriate. Links that require an
      // incoming and outgoing connection will not emit a LinkConnectedEvent until both connections are connected.
      .forEach(accountManager::createAccount);
  }

  /**
   * Helper to find the first account that should be used for IL-DCP.
   *
   * @return
   */
  private AccountSettings findParentAccountSettings() {
    return this.connectorSettingsSupplier.get().getAccountSettings().stream()
      .filter(accountSettings -> accountSettings.isParentAccount())
      .findFirst()
      .orElseThrow(() -> new RuntimeException(
        "At least one `parent` account must be defined if no operator address is specified at startup!"
      ));
  }

  /**
   * Helper method to initialize the parent account using IL-DCP. If this Connector is starting in `child` mode, it will
   * not have an operator address when it starts up, so this method finds the first account of type `parent` and use
   * IL-DCP to get the operating address for this Connector.
   *
   * @return The newly created parent {@link Account}.
   */
  private Account initializeParentAccount(final AccountSettings firstParentAccountSettings) {
    Objects.requireNonNull(firstParentAccountSettings);

    // Create the account to gain access to its Link.
    final Account createdParentAccount = accountManager.createAccount(firstParentAccountSettings);
    final IldcpResponse ildcpResponse = ((IldcpFetcher) ildcpRequest -> {
      Objects.requireNonNull(ildcpRequest);

      final IldcpRequestPacket ildcpRequestPacket = IldcpRequestPacket.builder().build();
      final InterledgerPreparePacket preparePacket =
        InterledgerPreparePacket.builder().from(ildcpRequestPacket).build();
      final InterledgerResponsePacket response = createdParentAccount.getLink().sendPacket(preparePacket);
      return new InterledgerResponsePacketMapper<IldcpResponse>() {
        @Override
        protected IldcpResponse mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
          return IldcpUtils.toIldcpResponse(interledgerFulfillPacket);
        }

        @Override
        protected IldcpResponse mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          throw new RuntimeException(String.format("IL-DCP negotiation failed! Reject: %s", interledgerRejectPacket));
        }
      }.map(response);

    }).fetch(IldcpRequest.builder().build());

    //////////////////////////////////
    // Update the ConnectorSettings...
    //////////////////////////////////

    // Update the Operator address
    ((ModifiableConnectorSettings) connectorSettingsSupplier.get())
      .setOperatorAddress(ildcpResponse.getClientAddress());
    // Update the Account Settings
    ((ModifiableAccountSettings) firstParentAccountSettings).setAssetCode(ildcpResponse.getAssetCode());
    ((ModifiableAccountSettings) firstParentAccountSettings).setAssetScale(ildcpResponse.getAssetScale());

    // Modify Account Settings by removing and re-creating the parent account.
    accountManager.removeAccount(createdParentAccount.getId());

    // Replace the AccountSettings with the values from IL-DCP.
    //    final AccountSettings updatedAccountSettings = AccountSettings.builder().from(firstParentAccountSettings)
    //      .assetScale(ildcpResponse.getAssetScale())
    //      .assetCode(ildcpResponse.getAssetCode())
    //      .build();
    return accountManager.createAccount(firstParentAccountSettings);
  }

}
