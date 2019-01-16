package com.sappenin.interledger.ilpv4.connector;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountSettingsResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.PluginManager;
import com.sappenin.interledger.ilpv4.connector.events.IlpNodeEvent;
import com.sappenin.interledger.ilpv4.connector.events.IlpNodeEventHandler;
import com.sappenin.interledger.ilpv4.connector.events.PluginConstructedEvent;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingService;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.factories.PluginFactoryProvider;
import org.interledger.plugin.lpiv2.events.PluginConnectedEvent;
import org.interledger.plugin.lpiv2.events.PluginDisconnectedEvent;
import org.interledger.plugin.lpiv2.events.PluginErrorEvent;
import org.interledger.plugin.lpiv2.events.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>A default implementation of {@link ILPv4Connector}.</p>
 *
 * <h1>Accounts and Plugins</h1>
 * <p>A Connector Account contains a Plugin, and cannot be created until the Plugin is created. The flow is that a
 * Plugin connects and then emits an {@link PluginConnectedEvent}, which this class responds to by constructing an
 * account and adding the account to the {@link AccountManager} for tracking.</p>
 *
 * <h1>^^ Account Manager ^^</h1>
 * <p>Connector accounts are managed in the Connector by the AccountManager. Accounts are added to the AccountManager
 * indirectly in response to a particular Plugin emitting a {@link PluginConnectedEvent}. This process can be initiated
 * during Connector startup (by configuring accounts in a properties file, database, or other mechanism); or at runtime
 * via a configuration mechanism (e.g., Admin API). Additionally, depending on the Plugin type, incoming connections can
 * be made to the Connector, which will trigger plugin creation and connection, and thus account tracking via the same
 * event mechanism.
 * </p>
 *
 * <h1>^^ Plugin Provider ^^</h1>
 * <p>Plugins are created dynamically in response to various events using a {@link PluginFactoryProvider}. To be
 * used by this connector, every plugin must have a corresponding factory class, which must be registered with the
 * factory provider.</p>
 *
 * <h1>^^ Plugin Factory ^^</h1>
 * <p>This is the primary way that plugins are be constructed. Each plugin type should have an eligible factory.</p>
 *
 * <h1>^^ Account Tracking ^^</h1>
 * When an account is constructed in response to a plugin connecting, the account is added to the {@link AccountManager}
 * for tracking. This makes the account eliglbe for routing, balance tracking, and packet-switching.
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
public class DefaultILPv4Connector implements ILPv4Connector, PluginEventListener {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final AccountIdResolver accountIdResolver;
  private final AccountSettingsResolver accountSettingsResolver;

  // TODO: Consider placing the PluginManager back inside of the AccountManager.
  private final AccountManager accountManager;
  private final PluginManager pluginManager;
  private final RoutingService routingService;

  // Handles all other packet addresses.
  private final ILPv4PacketSwitch ilpPacketSwitch;

  public DefaultILPv4Connector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountIdResolver accountIdResolver,
    final AccountSettingsResolver accountSettingsResolver,
    final AccountManager accountManager,
    final PluginManager pluginManager,
    final RoutingService routingService,
    final ILPv4PacketSwitch ilpPacketSwitch
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.accountSettingsResolver = Objects.requireNonNull(accountSettingsResolver);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.pluginManager = Objects.requireNonNull(pluginManager);
    this.routingService = Objects.requireNonNull(routingService);
    this.ilpPacketSwitch = Objects.requireNonNull(ilpPacketSwitch);
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
      //.filter(accountSettings -> accountSettings.getRelationship() == ? // PEER plugins might be of type 'client')
      // For-each AccountSettings, get or create a corresponding client Plugin and try to connect to the remote. If
      // successful, addAccount the account to the AccountManager for tracking.
      .forEach(accountSettings -> {
        final PluginSettings pluginSettings = PluginSettings.builder()
          .pluginType(accountSettings.getPluginType())
          .customSettings(accountSettings.getCustomSettings())
          .operatorAddress(connectorSettingsSupplier.get().getOperatorAddress())
          .build();
        final Plugin<?> plugin = pluginManager.createPlugin(accountSettings.getId(), pluginSettings);
        // Register the Connector as a PluginEvent Listener...
        plugin.addPluginEventListener(UUID.randomUUID(), this);
        try {
          // Try to connect, but only wait 15 seconds. Don't let one connection failure block the other plugins from
          // connecting.
          plugin.connect().get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
          throw new RuntimeException(
            String.format("Unable to connect Plugin: %s", pluginSettings), e);
        }
      });

    // TODO: Gate this with a config property...
    //this.initializePingProtocol();

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
  public RoutingService getRoutingService() {
    return this.routingService;
  }

  @Override
  public ILPv4PacketSwitch getIlpPacketSwitch() {
    return this.ilpPacketSwitch;
  }

  /**
   * Handles an incoming {@link InterledgerPreparePacket} received from a connected peer, but that may have originated
   * from any Interledger sender in the network.
   *
   * @param sourceAccountId       An {@link AccountId} for the remote peer (directly connected to this receiver) that
   *                              immediately sent the packet.
   * @param incomingPreparePacket A {@link InterledgerPreparePacket} containing data about an incoming payment.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingData(
    AccountId sourceAccountId, InterledgerPreparePacket incomingPreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(incomingPreparePacket);

    // Generally, plugins running in this connector will be the original source of an incoming prepare packet.
    // However, other systems (like a test-harness) might originate an incoming packet via this method.
    // In the case of plugin-originated packets, this implementation (i.e., the Connector) will always be the
    // registered handler for a plugin, which means incoming packets will show-up here. Thus, in the general case, this
    // method bridges between the incoming plugin, and the rest of the connector routing fabric. This
    // means we simply forward to the ilpPacketSwitch. However, in some cases, it might be the case that we route to
    // other switches.

    // NOTE: To handle packets addressed to _this_ node, the routing-table contains an entry for this connector, which
    // routes it to one or more plugins mapped to a `self.` prefix. Additionally, to handle packets addressed to the
    // `peer.` prefix, the routing-table contains entries for each supported peer-protocol.

    return this.ilpPacketSwitch.sendData(sourceAccountId, incomingPreparePacket);
  }

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);

    return this.ilpPacketSwitch.sendData(sourceAccountId, sourcePreparePacket);
  }

  @Override
  public InterledgerAddress getAddress() {
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
       * register the plugin's DataHandler to this Connector's sendData method, which will connect the newly
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
        //          (packet) -> sendData(event.getPlugin().getPluginSettings().getAccountAddress(), packet));
      }
    }.handle(event);
  }

  //  private void initializePingProtocol() {
  //    ////////////////
  //    // `self.ping` PROTOCOL
  //    ////////////////
  //
  //    // This account handles all `ping` protocol requests and responses. The account for this plugin is called `self
  //    // .ping`, which is an internal ILP account operated by this plugin. In other words, if you want to ping this
  //    // connector, you need to send a payment directly to the connector, and this account both segments that
  //    // handler from the typical Connector switching fabric, but also conveniently tracks those payments. In this way,
  //    // ping packets can drain liquidity from this Connector's peer that is forwarding the ping packets.
  //    final PluginSettings pluginSettings = ImmutablePluginSettings.builder()
  //      .pluginType(PingProtocolPlugin.PLUGIN_TYPE)
  //      .localNodeAddress(connectorSettingsSupplier.get().getOperatorAddress())
  //      .accountAddress(SELF_DOT_PING)
  //      .build();
  //    final AccountSettings accountSettings = ImmutableAccountSettings.builder()
  //      .pluginSettings(pluginSettings)
  //      .description("Plugin for handling ping-protocol packets.")
  //      // TODO: Reconsider this...
  //      .maximumPacketAmount(BigInteger.ZERO)
  //      // From the perspective of the node operator, the _other_ node is the child of this node.
  //      .relationship(AccountRelationship.CHILD)
  //      .build();
  //    this.getAccountManager().addAccount(accountSettings);
  //
  //    // For any packets destined to this Connector, we route them to the `self.ping` account.
  //    final Route route = ImmutableRoute.builder()
  //      .routePrefix(InterledgerAddressPrefix.from(connectorSettingsSupplier.get().getOperatorAddress()))
  //      .nextHopAccount(SELF_DOT_PING)
  //      .build();
  //    this.getRoutingService().getRoutingTable().addRoute(route);
  //
  //    ////////////////
  //    // `peer.config` PROTOCOL
  //    ////////////////
  //
  //    ////////////////
  //    // `peer.route` PROTOCOL
  //    ////////////////
  //    // A connector should be able to speak CCP with its peers. To do this, we utilize an Internally-routed plugin,
  //    // which handles all incoming traffic for the `peer.route` address.
  //
  //  }

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

  /**
   * When a {@link Plugin} connects, we need to begin tracking the associated account using the {@link
   * #accountManager}.
   *
   * @param event A {@link PluginConnectedEvent}.
   */
  @Override
  public void onConnect(final PluginConnectedEvent event) {
    Objects.requireNonNull(event);

    final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getPlugin());
    final AccountSettings accountSettings = this.accountSettingsResolver.resolveAccountSettings(accountId);
    accountManager.addAccount(
      Account.builder()
        //.id(accountId) // Found in AccountSettings.
        .accountSettings(accountSettings)
        .plugin(event.getPlugin())
        .build()
    );

    // TODO: Make the AccountManager handle this connection? Currently, the RoutingService _has an_ AccountManager, so
    // we don't really want to introduce a circular dependency here. However, it does seem natural that the
    // AccountManager should choreograph everything in the RoutingService, rather than having any listeners in the
    // Routing Service.

    // Register this account with the routing service...this won't work because the RoutingService won't start
    // tracking until the plugin connects, but by this point in time, the plugin has already connected!
    //routingService.registerAccount(accountId);
  }

  /**
   * Called to handle an {@link PluginDisconnectedEvent}. When a {@link Plugin} disconnects, we need to stop tracking
   * the associated account using the {@link #accountManager}.
   *
   * @param event A {@link PluginDisconnectedEvent}.
   */
  @Override
  public void onDisconnect(final PluginDisconnectedEvent event) {
    Objects.requireNonNull(event);

    final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getPlugin());

    // TODO: See comment above about merging Routing registration into AccountMangager.
    // Remove the account from any other consideration.
    // Unregister this account with the routing service...
    //this.routingService.unregisterAccount(accountId);

    this.accountManager.removeAccount(accountId);
  }

  /**
   * Called to handle an {@link PluginErrorEvent}.
   *
   * @param event A {@link PluginErrorEvent}.
   */
  @Override
  public void onError(final PluginErrorEvent event) {
    Objects.requireNonNull(event);
    logger.error("Plugin: {}; PluginError: {}", event.getPlugin(), event.getError());
  }
}
