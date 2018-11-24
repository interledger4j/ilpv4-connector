package com.sappenin.interledger.ilpv4.connector;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.model.events.IlpNodeEvent;
import com.sappenin.interledger.ilpv4.connector.model.events.IlpNodeEventHandler;
import com.sappenin.interledger.ilpv4.connector.model.events.PluginConstructedEvent;
import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.model.settings.ImmutableAccountSettings;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.plugins.connectivity.PingProtocolPlugin;
import com.sappenin.interledger.ilpv4.connector.routing.ImmutableRoute;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingService;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.plugin.lpiv2.settings.ImmutablePluginSettings;
import org.interledger.plugin.lpiv2.settings.PluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.plugins.connectivity.PingProtocolPlugin.SELF_DOT_PING;

/**
 * A default implementation of {@link ILPv4Connector}.
 */
public class DefaultILPv4Connector implements ILPv4Connector {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final AccountManager accountManager;
  private final RoutingService routingService;

  // Handles all other packet addresses.
  private final ILPv4PacketSwitch ilpPacketSwitch;

  public DefaultILPv4Connector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager, final RoutingService routingService,
    final ILPv4PacketSwitch ilpPacketSwitch
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);

    this.accountManager = Objects.requireNonNull(accountManager);
    this.routingService = Objects.requireNonNull(routingService);

    this.ilpPacketSwitch = Objects.requireNonNull(ilpPacketSwitch);
  }

  /**
   * Initialize the connector after constructing it.
   */
  @PostConstruct
  private final void init() {

    // TODO: Gate this with a config property...
    this.initializePingProtocol();

    // TODO: Enable these.
    //this.initializePeerConfigProtocol();
    //this.initializePeerRouteProtocol();
  }

  @PreDestroy
  public void shutdown() {
    // accountManager#shutdown is called automatically by spring due to naming convention, so no need to call it here.
  }

  // TODO: Make this a supplier.
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
   * @param sourceAccountAddress  An {@link InterledgerAddress} for the remote peer (directly connected to this
   *                              receiver) that immediately sent the packet.
   * @param incomingPreparePacket A {@link InterledgerPreparePacket} containing data about an incoming payment.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingData(
    InterledgerAddress sourceAccountAddress, InterledgerPreparePacket incomingPreparePacket
  ) {
    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(incomingPreparePacket);

    // Generally, plugins running in this connector will be the original source of an incoming prepare packet.
    // However, other systems (like a test-harness) might originate an incoming packet via this method.
    // In the case of plugin-originated packets, this implementation (i.e., the Connector) will always be the
    // registered handler for a plugin, which means incoming packets will show-up here. Thus, in the general case, this
    // method bridges between the incoming plugin, and the rest of the connector routing fabric. In general, this
    // means we simply forward to the ilpPacketSwitch. However, in some cases, it might be the case that we route to
    // other switches.

    // NOTE: To handle packets addressed to _this_ node, the routing-table contains an entry for this connector, which
    // routes it to one or more plugins mapped to a `self.` prefix.

    // Additionally, to handle packets addressed to the `peer.` prefix, the routing-table contains entries for each
    // supported peer-protocol.

    return this.ilpPacketSwitch.sendData(sourceAccountAddress, incomingPreparePacket);
  }

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(sourcePreparePacket);

    return this.ilpPacketSwitch.sendData(sourceAccountAddress, sourcePreparePacket);
  }

  @Override
  public InterledgerAddress getAddress() {
    return this.connectorSettingsSupplier.get().getIlpAddress();
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
        // Unregister both handlers, just in-case the plugin itself sets its own handler (e.g., SimulatedPlugin)
        event.getPlugin().unregisterDataHandler();
        event.getPlugin().unregisterMoneyHandler();

        // Plugins talk to other plugins. Thus, when a plugin has an incoming data-packet (i.e., from the remote plugin)
        // this handler should be invoked in order to route this packet back to the caller.
        event.getPlugin().registerDataHandler(
          (packet) -> sendData(event.getPlugin().getPluginSettings().getPeerAccountAddress(), packet));
      }
    }.handle(event);
  }

  private void initializePingProtocol() {
    ////////////////
    // `self.ping` PROTOCOL
    ////////////////

    // This account handles all `ping` protocol requests and responses. The account for this plugin is called `self
    // .ping`, which is an internal ILP account operated by this plugin. In other words, if you want to ping this
    // connector, you need to send a payment directly to the connector, and this account both segments that
    // handler from the typical Connector switching fabric, but also conveniently tracks those payments. In this way,
    // ping packets can drain liquidity from this Connector's peer that is forwarding the ping packets.
    final PluginSettings pluginSettings = ImmutablePluginSettings.builder()
      .pluginType(PingProtocolPlugin.PLUGIN_TYPE)
      .localNodeAddress(connectorSettingsSupplier.get().getIlpAddress())
      .peerAccountAddress(SELF_DOT_PING)
      .build();
    final AccountSettings accountSettings = ImmutableAccountSettings.builder()
      .pluginSettings(pluginSettings)
      .description("Plugin for handling ping-protocol packets.")
      // TODO: Reconsider this...
      .maximumPacketAmount(BigInteger.ZERO)
      // From the perspective of the node operator, the _other_ node is the child of this node.
      .relationship(AccountSettings.AccountRelationship.CHILD)
      .build();
    this.getAccountManager().add(accountSettings);

    // For any packets destined to this Connector, we route them to the `self.ping` account.
    final Route route = ImmutableRoute.builder()
      .routePrefix(InterledgerAddressPrefix.from(connectorSettingsSupplier.get().getIlpAddress()))
      .nextHopAccount(SELF_DOT_PING)
      .build();
    this.getRoutingService().getRoutingTable().addRoute(route);

    ////////////////
    // `peer.config` PROTOCOL
    ////////////////

    ////////////////
    // `peer.route` PROTOCOL
    ////////////////
    // A connector should be able to speak CCP with its peers. To do this, we utilize an Internally-routed plugin,
    // which handles all incoming traffic for the `peer.route` address.

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

}
