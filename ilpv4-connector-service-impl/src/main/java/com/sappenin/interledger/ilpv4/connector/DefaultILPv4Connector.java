package com.sappenin.interledger.ilpv4.connector;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.PluginManager;
import com.sappenin.interledger.ilpv4.connector.events.IlpNodeEvent;
import com.sappenin.interledger.ilpv4.connector.events.IlpNodeEventHandler;
import com.sappenin.interledger.ilpv4.connector.events.PluginConstructedEvent;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.plugins.connectivity.PingProtocolPlugin;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import com.sappenin.interledger.ilpv4.connector.settings.AccountRelationship;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.plugin.lpiv2.btp2.spring.factories.PluginFactoryProvider;
import org.interledger.plugin.lpiv2.events.PluginConnectedEvent;
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
 * <h1>Accounts and Plugins</h1>
 * <p>A Connector Account contains a Plugin, and cannot be created until the Plugin is created. For some
 * plugin/account combinations, this means that a Plugin must first connect, and then emits a {@link
 * PluginConnectedEvent}, the Connector will respond to by constructing and beinging to manager an Account. In other
 * cases, an Account is pre-configured in this node, and thus triggers the creation of a plugin which connects to a
 * remote server (the PluginConnectedEvent process is the same in this case).
 * </p>
 *
 * <h1>^^ Account Manager ^^</h1>
 * <p>Connector accounts are managed by the {@link AccountManager}. Accounts can be added to the AccountManager
 * indirectly in response to a particular Plugin emitting a {@link PluginConnectedEvent}. This process can be initiated
 * during Connector startup (by configuring accounts in a properties file, database, or other mechanism); or at runtime
 * via a configuration mechanism (e.g., Admin API). Additionally, depending on the Plugin type, incoming connections can
 * be made to the Connector, which will trigger plugin creation and connection, and thus account tracking via the same
 * event mechanism.
 * </p>
 *
 * <h1>^^ Plugin Provider ^^</h1>
 * <p>Some plugin types are created dynamically in response to various events using a {@link PluginFactoryProvider}.
 * To be used by this connector, every plugin must have a corresponding factory class, which must be registered with the
 * factory provider.</p>
 *
 * <h1>^^ Plugin Factory ^^</h1>
 * <p>This is the primary way that plugins are be constructed. Each plugin type should have an eligible factory.</p>
 *
 * <h1>^^ Account Tracking ^^</h1>
 * When an account is constructed in response to a plugin connecting, the account is added to the {@link AccountManager}
 * for tracking. This makes the account eligible for balance tracking, packet-switching, and route-propagation via the
 * CCP.
 *
 * <h2>^^ Server Account Tracking ^^</h2>
 * Some plugins support multiple accounts, such as a BTP WebSocket Server. For each connection the server accepts, a new
 * plugin will be instantiated and its `connect` method called. This will trigger a {@link PluginConnectedEvent} that
 * the connector will listen to, and instruct the AccountManager to begin tracking this account. If the plugin
 * disconnects, this Connector will be listening, and instruct the AccountManager to stop tracking the account.
 *
 * <h2>^^ Client Account Tracking ^^</h2>
 * Other plugins initiate an outbound connection to a remote server. For example, a BTP Client plugin will do this, and
 * it may not receive its account information until _after_ the plugin has connected. Thus, this Connector listens for
 * the same {@link PluginConnectedEvent} events as above, and tracks or untracks each Account based upon what the
 * corresponding Plugin does.
 */
public class DefaultILPv4Connector implements ILPv4Connector {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final EventBus eventBus;

  // TODO: Consider placing the PluginManager back inside of the AccountManager.
  private final AccountManager accountManager;
  private final PluginManager pluginManager;

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
    final PluginManager pluginManager,
    final InternalRoutingService internalRoutingService,
    final ExternalRoutingService externalRoutingService,
    final ILPv4PacketSwitch ilpPacketSwitch
  ) {
    this(
      connectorSettingsSupplier,
      accountManager,
      pluginManager,
      internalRoutingService,
      externalRoutingService,
      ilpPacketSwitch,
      new EventBus()
    );
  }

  public DefaultILPv4Connector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final PluginManager pluginManager,
    final InternalRoutingService internalRoutingService, final ExternalRoutingService externalRoutingService,
    final ILPv4PacketSwitch ilpPacketSwitch,
    final EventBus eventBus
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.pluginManager = Objects.requireNonNull(pluginManager);
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
    // For any pre-configured accounts, we need to construct their corresponding plugin and `connect` it so it will
    // be added to the AccountManager for proper tracking.
    this.connectorSettingsSupplier.get().getAccountSettings().stream()
      // Connect all plugins, regardless of type. Server-plugins won't be defined in here, and if there's a peer plugin
      // that this connector is a client of, then it will be connected here, if appropriate. Plugins that require an
      // incoming and outgoing connection will not emit a PluginConnectedEvent until both connections are connected.
      .forEach(accountManager::createAccount);

    ////////////////////////////////////////
    // Enable ILP and other Protocol Plugins
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
    // Shutdown all plugins...This will emit PluginDisconnected events that will be handled below...
    this.accountManager.getAllAccounts().forEach(account -> {
      account.getPlugin().disconnect();
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
  public PluginManager getPluginManager() {
    return this.pluginManager;
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
  //  public CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
  //    final AccountId accountId, final InterledgerPreparePacket preparePacket
  //  ) {
  //    Objects.requireNonNull(accountId);
  //    Objects.requireNonNull(preparePacket);
  //    return this.ilpPacketSwitch.routeData(accountId, preparePacket);
  //  }

  //  @Override
  //  public CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingData(
  //    AccountId sourceAccountId, InterledgerPreparePacket incomingPreparePacket
  //  ) {
  //    Objects.requireNonNull(sourceAccountId);
  //    Objects.requireNonNull(incomingPreparePacket);
  //
  //    // Generally, plugins running in this connector will be the original source of an incoming prepare packet.
  //    // However, other systems (like a test-harness) might originate an incoming packet via this method.
  //    // In the case of plugin-originated packets, this implementation (i.e., the Connector) will always be the
  //    // registered handler for a plugin, which means incoming packets will show-up here. Thus, in the general case, this
  //    // method bridges between the incoming plugin, and the rest of the connector routing fabric. This
  //    // means we simply forward to the ilpPacketSwitch. However, in some cases, it might be the case that we route to
  //    // other switches.
  //
  //    // NOTE: To handle packets addressed to _this_ node, the routing-table contains an entry for this connector, which
  //    // routes it to one or more plugins mapped to a `self.` prefix. Additionally, to handle packets addressed to the
  //    // `peer.` prefix, the routing-table contains entries for each supported peer-protocol.
  //    return this.ilpPacketSwitch.sendData(sourceAccountId, incomingPreparePacket);
  //  }

  @Override
  public InterledgerAddress getNodeIlpAddress() {
    return this.connectorSettingsSupplier.get().getOperatorAddress();
  }

  /**
   * Handle an event of type {@link IlpNodeEvent}.
   *
   * @param event the event to respond to.
   */
  @EventListener
  public void onApplicationEvent(final IlpNodeEvent event) {
    Objects.requireNonNull(event);

    new IlpNodeEventHandler() {

      /**
       * When a plugin is constructed, it will emit this event. The Connector listens for this event and uses it to
       * register the plugin's DataHandler to this Connector's routeData method, which will connect the newly
       * constructed plugin to this Connector's switching fabric.
       *
       * @param event A  {@link PluginConstructedEvent} to be responded to.
       */
      @Override
      protected void handlePluginConstructedEvent(PluginConstructedEvent event) {

        // TODO: When a plugin is constructed, we don't really care. What we really care about is when it connects.
        // When it connects, we need to both register the DataHandler, _and_ addAccount when it disconnects, we probably
        // want to unregister the datahandler (though this may not strictly be necessary).
        throw new RuntimeException("FIXME - This is wrong!");

        //
        //        // Unregister both handlers, just in-case the plugin itself sets its own handler (e.g., SimulatedPlugin)
        //        event.getPlugin().unregisterDataHandler();
        //        event.getPlugin().unregisterMoneyHandler();
        //
        //        // Plugins talk to other plugins. Thus, when a plugin has an incoming data-packet (i.e., from the remote plugin)
        //        // this handler should be invoked in order to route this packet back to the caller.
        //        event.getPlugin().registerDataHandler(
        //          (packet) -> routeData(event.getPlugin().getPluginSettings().getAccountAddress(), packet));
      }
    }.handle(event);
  }

  /**
   * <p>Enables support for the `PING` and `ECHO` protocols by installing entries into the internal routing table and
   * configuring the account-manager properly.</p>
   *
   * <p>Internal routing of this type of packet works as follows:
   * <ol>
   * <li>A packet addressed to this Connector enters the switching fabric.</li>
   * <li>The internal routing table has an entry that forwards the packet to a local account associated to an
   * instance of the Ping Plugin.</li>
   * <li>The Ping Plugin handles the packet appropriately.</li>
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
      .pluginType(PingProtocolPlugin.PLUGIN_TYPE)
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
    // A connector should be able to speak CCP with its peers. To do this, we utilize an Internally-routed plugin,
    // which handles all incoming traffic for the `peer.route` address.

  }

  ////////////////////////
  // Plugin Event Listener
  ////////////////////////

  //  /**
  //   * When a {@link Plugin} connects, we need to begin tracking the associated account using the {@link
  //   * #accountManager}.
  //   *
  //   * @param event A {@link PluginConnectedEvent}.
  //   */
  //  @Override
  //  @Subscribe
  //  public void onConnect(final PluginConnectedEvent event) {
  //    Objects.requireNonNull(event);
  //
  ////    final AccountSettings accountSettings = this.accountSettingsResolver.resolveAccountSettings(event.getPlugin());
  ////    accountManager.addAccount(
  ////      Account.builder()
  ////        //.id(accountId) // Found in AccountSettings.
  ////        .accountSettings(accountSettings)
  ////        .plugin(event.getPlugin())
  ////        .build()
  ////    );
  //
  ////    // Register this account with the routing service...this won't work because the ExternalRoutingService won't start
  ////    // tracking until the plugin connects, but by this point in time, the plugin has already connected!
  ////    final AccountSettings accountSettings = this.accountSettingsResolver.resolveAccountSettings(event.getPlugin());
  ////    externalRoutingService.registerAccount(accountSettings.getId());
  //  }

  //  /**
  //   * Called to handle an {@link PluginDisconnectedEvent}. When a {@link Plugin} disconnects, we need to stop tracking
  //   * the associated account using the {@link #accountManager}.
  //   *
  //   * @param event A {@link PluginDisconnectedEvent}.
  //   */
  //  @Override
  //  @Subscribe
  //  public void onDisconnect(final PluginDisconnectedEvent event) {
  //    Objects.requireNonNull(event);
  //
  //    //final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getPlugin());
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
