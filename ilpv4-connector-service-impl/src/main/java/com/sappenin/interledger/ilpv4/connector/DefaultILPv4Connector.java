package com.sappenin.interledger.ilpv4.connector;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import com.sappenin.interledger.ilpv4.connector.events.IlpNodeEvent;
import com.sappenin.interledger.ilpv4.connector.links.ping.PingProtocolLink;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.UUID;
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
    final ILPv4PacketSwitch ilpPacketSwitch
  ) {
    this(
      connectorSettingsSupplier,
      accountManager,
      linkManager,
      internalRoutingService,
      externalRoutingService,
      ilpPacketSwitch,
      new EventBus()
    );
  }

  public DefaultILPv4Connector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final LinkManager linkManager,
    final InternalRoutingService internalRoutingService, final ExternalRoutingService externalRoutingService,
    final ILPv4PacketSwitch ilpPacketSwitch,
    final EventBus eventBus
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.internalRoutingService = Objects.requireNonNull(internalRoutingService);
    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
    this.ilpPacketSwitch = Objects.requireNonNull(ilpPacketSwitch);
    this.eventBus = Objects.requireNonNull(eventBus);
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
    final EnabledProtocolSettings enabledProtocolSettings = connectorSettingsSupplier.get().getEnabledProtocols();

    if (enabledProtocolSettings.isPingProtocolEnabled()) {
      this.initializePingEchoProtocol();
    }
    // TODO: re-enable after RFC
    //    if (enabledProtocolSettings.isPingProtocolEnabled()) {
    //      this.enableEchoProtocol();
    //    }

    // TODO: Enable these.
    //this.initializePeerConfigProtocol();
    //this.initializePeerRouteProtocol();
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
  public ILPv4PacketSwitch getIlpPacketSwitch() {
    return this.ilpPacketSwitch;
  }

  // TODO: Move this to an admin interface that this Connector _has_. Consider making this connector be part of a
  //  ConnectoAdministrative service.
  //  It should not be a part of the Connector's
  //  business logic proper, because the only thing a Connector does is handle an administratively triggered packet.

  // From the perspective of a Connector/Switch, it's only ever handling incoming data from an account, and then
  // routing it to another account.
  //  @Override
  //  public CompletableFuture<Optional<InterledgerResponsePacket>> sendPacket(
  //    final AccountId accountId, final InterledgerPreparePacket preparePacket
  //  ) {
  //    Objects.requireNonNull(accountId);
  //    Objects.requireNonNull(preparePacket);
  //    return this.ilpPacketSwitch.routeData(accountId, preparePacket);
  //  }

  //  @Override
  //  public CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingPacket(
  //    AccountId sourceAccountId, InterledgerPreparePacket incomingPreparePacket
  //  ) {
  //    Objects.requireNonNull(sourceAccountId);
  //    Objects.requireNonNull(incomingPreparePacket);
  //
  //    // Generally, links running in this connector will be the original source of an incoming prepare packet.
  //    // However, other systems (like a test-harness) might originate an incoming packet via this method.
  //    // In the case of link-originated packets, this implementation (i.e., the Connector) will always be the
  //    // registered handler for a link, which means incoming packets will show-up here. Thus, in the general case, this
  //    // method bridges between the incoming link, and the rest of the connector routing fabric. This
  //    // means we simply forward to the ilpPacketSwitch. However, in some cases, it might be the case that we route to
  //    // other switches.
  //
  //    // NOTE: To handle packets addressed to _this_ node, the routing-table contains an entry for this connector, which
  //    // routes it to one or more links mapped to a `self.` prefix. Additionally, to handle packets addressed to the
  //    // `peer.` prefix, the routing-table contains entries for each supported peer-protocol.
  //    return this.ilpPacketSwitch.sendPacket(sourceAccountId, incomingPreparePacket);
  //  }

  @Override
  public InterledgerAddress getNodeIlpAddress() {
    return this.connectorSettingsSupplier.get().getOperatorAddress();
  }

  /**
   * Handle an event of type {@link IlpNodeEvent}.
   *
   * @param event the event to respond to.
   *
   * @deprecated Determine if this method is necessary. It may be desirable to instead use the EventBus.
   */
  @EventListener
  @Deprecated
  public void onApplicationEvent(final IlpNodeEvent event) {

  }

  /**
   * <p>Enables support for the `PING` and `ECHO` protocols by installing entries into the internal routing table and
   * configuring the account-manager properly.</p>
   *
   * <p>Internal routing of this type of packet works as follows:
   * <ol>
   * <li>A packet addressed to this Connector enters the switching fabric.</li>
   * <li>The internal routing table has an entry that forwards the packet to a local account associated to an
   * instance of the Ping Link.</li>
   * <li>The Ping Link handles the packet appropriately.</li>
   * </ol>
   * </p>
   */
  private void initializePingEchoProtocol() {
    ////////////////
    // PING PROTOCOL
    ////////////////
    final UUID pingAccountUuid = UUID.randomUUID();
    final AccountId accountId = AccountId.of(pingAccountUuid.toString());

    final Route internalRoute = Route.builder()
      // Never expires.
      // No Auth needed (these routes are not propagated externally via CCP).
      .routePrefix(InterledgerAddressPrefix.from(connectorSettingsSupplier.get().getOperatorAddress()))
      .nextHopAccountId(accountId)
      .build();
    this.internalRoutingService.addRoute(internalRoute);

    final AccountSettings accountSettings = AccountSettings.builder()
      .id(accountId)
      .isPreconfigured(true)
      .relationship(AccountRelationship.CHILD)
      .assetScale(9)
      .assetCode("USD")
      .linkType(PingProtocolLink.LINK_TYPE)
      .build();
    this.accountManager.createAccount(accountSettings);
  }

  private void initializePeerConfigProtocol() {

    ////////////////
    // `peer.config` PROTOCOL
    ////////////////

  }

  private void initializePeerRouteProtocol() {
    ////////////////
    // `peer.route` PROTOCOL
    ////////////////
    // A connector should be able to speak CCP with its peers. To do this, we utilize an Internally-routed link,
    // which handles all incoming traffic for the `peer.route` address.

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
