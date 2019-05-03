package com.sappenin.interledger.ilpv4.connector;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.events.IlpNodeEvent;
import com.sappenin.interledger.ilpv4.connector.links.LinkSettingsFactory;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>A default implementation of an {@link ILPv4Connector}.</p>
 *
 * <h1>Accounts and Links</h1>
 * <p>A Connector Account can become connected to this Connector using a Link. Depending on the Link type, some
 * accounts initiate a connection to a remote node when the Connector starts-up, whereas other accounts do not become
 * connected until the remote node initiates a connection to the Connector. In either case, when a Link becomes
 * connected, the Connector will emit a {@link LinkConnectedEvent} that some of the Connector's other services will
 * listen for and respond to.
 * </p>
 *
 * <h1>^^ Account Settings Repository ^^</h1>
 * <p>Connector accounts are primarily managed by the {@link AccountSettingsRepository}, which tracks only settings
 * for each account.</p>
 *
 * <h1>^^ Link Provider ^^</h1>
 * <p>Some Link types are created dynamically in response to various events using a {@link LinkFactoryProvider}.
 * To be used by this connector, every link must have a corresponding factory class, which must be registered with the
 * factory provider.</p>
 *
 * <h1>^^ Link Factory ^^</h1>
 * <p>This is the primary way that Links are be constructed. Each type of Link should have an eligible factory.</p>
 *
 * <h1>^^ Account Tracking ^^</h1>
 * When an account is constructed in response to a Link connecting, the account is added to the {@link LinkManager} for
 * tracking. This makes the account eligible for balance tracking, packet-switching, route-propagation via the CCP, and
 * more. If the link disconnects, any listening services will react properly to stop tracking the Account/Link
 * combination.
 */
public class DefaultILPv4Connector implements ILPv4Connector {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final AccountManager accountManager;
  private final AccountSettingsRepository accountSettingsRepository;
  private final LinkManager linkManager;
  private final LinkSettingsFactory linkSettingsFactory;

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
    final AccountSettingsRepository accountSettingsRepository,
    final LinkManager linkManager,
    final InternalRoutingService internalRoutingService,
    final ExternalRoutingService externalRoutingService,
    final ILPv4PacketSwitch ilpPacketSwitch,
    final BalanceTracker balanceTracker,
    final LinkSettingsFactory linkSettingsFactory
  ) {
    this(
      connectorSettingsSupplier,
      accountManager,
      accountSettingsRepository,
      linkManager,
      internalRoutingService,
      externalRoutingService,
      ilpPacketSwitch,
      balanceTracker,
      linkSettingsFactory,
      new EventBus()
    );
  }

  public DefaultILPv4Connector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final AccountSettingsRepository accountSettingsRepository,
    final LinkManager linkManager,
    final InternalRoutingService internalRoutingService,
    final ExternalRoutingService externalRoutingService,
    final ILPv4PacketSwitch ilpPacketSwitch,
    final BalanceTracker balanceTracker,
    final LinkSettingsFactory linkSettingsFactory,
    final EventBus eventBus
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.accountSettingsRepository = accountSettingsRepository;
    this.linkManager = Objects.requireNonNull(linkManager);
    this.internalRoutingService = Objects.requireNonNull(internalRoutingService);
    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
    this.ilpPacketSwitch = Objects.requireNonNull(ilpPacketSwitch);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
    this.linkSettingsFactory = Objects.requireNonNull(linkSettingsFactory);
    Objects.requireNonNull(eventBus).register(this);
  }

  /**
   * <p>Initialize the connector after constructing it.</p>
   */
  @PostConstruct
  private final void init() {
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
    this.linkManager.getAllConnectedLinks().forEach(Link::disconnect);
  }

  @Override
  public ConnectorSettings getConnectorSettings() {
    return this.connectorSettingsSupplier.get();
  }

  @Override
  public AccountSettingsRepository getAccountSettingsRepository() {
    return accountSettingsRepository;
  }

  @Override
  public LinkManager getLinkManager() {
    return this.linkManager;
  }

  @Override
  public AccountManager getAccountManager() {
    return this.accountManager;
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

  /**
   * Configure the parent account for this Connector using IL-DCP.
   */
  private void configureAccountsUsingIldcp() {
    /////////////////////////////////
    // ^^ Parent Account ^^
    /////////////////////////////////
    // If this Connector is starting in `child` mode, it will not have an operator address. We need to find the first
    // account of type `parent` and use IL-DCP to get the operating account for this Connector.
    final Optional<AccountSettingsEntity> primaryParentAccountSettings =
      this.accountSettingsRepository.findPrimaryParentAccountSettings();
    if (primaryParentAccountSettings.isPresent()) {
      // If there's no Operator Address, use IL-DCP to try to get one. Only try this once. If this fails, then the
      // Connector should not startup.
      this.accountManager.initializeParentAccountSettingsViaIlDcp(primaryParentAccountSettings.get());
      logger.info(
        "IL-DCP Succeeded! Operator Address: `{}`", connectorSettingsSupplier.get().getOperatorAddress().get()
      );
    } else {
      logger.warn("At least one `parent` account must be defined if no operator address is specified at startup. " +
        "Please set the operator address or else add a new account of type `PARENT`");
    }
  }

  /**
   * Configure any Accounts that are the Link connection initiator.
   */
  private void configureAccounts() {
    // Connect any Links for accounts that are the connection initiator. Links that require an incoming and outgoing
    // connection will emit a LinkConnectedEvent when the incoming connection is connected.
    this.accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrue().stream()
      .forEach(accountSettings -> {
        final LinkSettings linkSettings = linkSettingsFactory.construct(accountSettings);
        final Link<?> link = linkManager.createLink(accountSettings.getAccountId(), linkSettings);

        ////////////////////////////
        // Connect the Link.
        ////////////////////////////
        // Don't allow one link to block another link from connecting. The internal connection logic of each link
        // will employ a timeout, so no need to use CompletableFuture timeouts here. Also, Link connection errors
        // will be caught an emitted by any listeners.
        link.connect();
      });
  }

}
