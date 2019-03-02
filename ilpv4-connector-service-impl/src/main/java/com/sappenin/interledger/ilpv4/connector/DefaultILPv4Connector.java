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
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.core.InterledgerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
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
    final BalanceTracker balanceTracker) {
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

    // ^^ Preconfigurated Accounts ^^
    // For any pre-configured accounts, we need to construct their corresponding link and `connect` it so it will
    // be added to the AccountManager for proper tracking.
    this.connectorSettingsSupplier.get().getAccountSettings().stream()
      // Connect all links, regardless of type. Server-links won't be defined in here, and if there's a peer link
      // that this connector is a client of, then it will be connected here, if appropriate. Links that require an
      // incoming and outgoing connection will not emit a LinkConnectedEvent until both connections are connected.
      .forEach(accountManager::createAccount);

    ////////////////////////////////////////
    // Enable ILP and other Protocol Links
    ////////////////////////////////////////
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
  public InterledgerAddress getNodeIlpAddress() {
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

  ////////////////////////
  // Link Event Listener
  ////////////////////////

  //  /**
  //   * When a {@link Link} connects, we need to begin tracking the associated account using the {@link
  //   * #accountManager}.
  //   *
  //   * @param event A {@link LinkConnectedEvent}.
  //   */
  //  @Override
  //  @Subscribe
  //  public void onConnect(final LinkConnectedEvent event) {
  //    Objects.requireNonNull(event);
  //
  ////    final AccountSettings accountSettings = this.accountSettingsResolver.resolveAccountSettings(event.getLink());
  ////    accountManager.addAccount(
  ////      Account.builder()
  ////        //.id(accountId) // Found in AccountSettings.
  ////        .accountSettings(accountSettings)
  ////        .link(event.getLink())
  ////        .build()
  ////    );
  //
  ////    // Register this account with the routing service...this won't work because the ExternalRoutingService won't start
  ////    // tracking until the link connects, but by this point in time, the link has already connected!
  ////    final AccountSettings accountSettings = this.accountSettingsResolver.resolveAccountSettings(event.getLink());
  ////    externalRoutingService.registerAccount(accountSettings.getId());
  //  }

  //  /**
  //   * Called to handle an {@link LinkDisconnectedEvent}. When a {@link Link} disconnects, we need to stop tracking
  //   * the associated account using the {@link #accountManager}.
  //   *
  //   * @param event A {@link LinkDisconnectedEvent}.
  //   */
  //  @Override
  //  @Subscribe
  //  public void onDisconnect(final LinkDisconnectedEvent event) {
  //    Objects.requireNonNull(event);
  //
  //    //final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getLink());
  //
  //    // TODO: See comment above about merging Routing registration into AccountMangager.
  //    // Remove the account from any other consideration.
  //    // Unregister this account with the routing service...
  //    //this.externalRoutingService.unregisterAccount(accountId);
  //
  //    //this.accountManager.removeAccount(accountId);
  //  }


  //  private void updateConnectorSettings(final ConnectorSettings connectorSettings) {
  //    Objects.requireNonNull(connectorSettings);
  //
  //    //    final ApplicationContext applicationContext = SpringContext.getApplicationContext();
  //
  //    //    applicationContext.getAutowireCapableBeanFactory()
  //    //
  //    //    if (applicationContext.getBean(ConnectorSettings.OVERRIDE_BEAN_NAME) != null) {
  //    //      return () -> (ConnectorSettings) applicationContext.getBean(ConnectorSettings.OVERRIDE_BEAN_NAME);
  //    //    } else {
  //    //      // No override was detected, so return the normal variant that exists because of the EnableConfigurationProperties
  //    //      // directive above.
  //    //      return () -> applicationContext.getBean(ConnectorSettings.class);
  //    //    }
  //
  //
  //    //    this.connectorSettingsOverride.ifPresent(cso -> {
  //    //      final BeanDefinitionRegistry registry = (
  //    //        (BeanDefinitionRegistry) this.getContext().getAutowireCapableBeanFactory()
  //    //      );
  //    //
  //    //      try {
  //    //        registry.removeBeanDefinition(ConnectorSettingsFromPropertyFile.BEAN_NAME);
  //    //      } catch (NoSuchBeanDefinitionException e) {
  //    //        // Swallow...
  //    //        logger.warn(e.getMessage(), e);
  //    //      }
  //    //
  //    //      // Replace here...
  //    //      this.getContext().getBeanFactory().registerSingleton(ConnectorSettings.BEAN_NAME, cso);
  //    //    });
  //
  //
  //    //    this.connectorSettingsOverride
  //    //      .ifPresent(cso -> ((ApplicationPreparedEvent) event).getApplicationContext().getBeanFactory()
  //    //        .registerSingleton(ConnectorSettings.OVERRIDE_BEAN_NAME, cso));
  //  }


}
