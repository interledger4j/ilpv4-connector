package org.interledger.connector.routing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.ccp.CcpConstants;
import org.interledger.connector.ccp.CcpRouteControlRequest;
import org.interledger.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.connector.ccp.CcpSyncMode;
import org.interledger.connector.ccp.codecs.CcpCodecContextFactory;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.AbstractStatefulLink;
import org.interledger.link.AbstractStatefulLink.EventBusConnectionEventEmitter;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.events.LinkConnectionEventEmitter;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Exercises Ccp Sender/Receiver functionality from a Java-only perspective. This test simulates two connectors, A and
 * B, operating both sides of a CcpRouting relationship in which Node B advertises routes to two other nodes, C and D,
 * like this:
 *
 * <pre>
 * ┌─────────┐      ┌─────────┐      ┌─────────┐      ┌─────────┐
 * │   t.a   │─ ─ ─ │   t.b   │─ ─ ─ │   t.c   │─ ─ ─ │   t.d   │
 * └─────────┘      └─────────┘      └─────────┘      └─────────┘
 * </pre>
 *
 * This harness exercises simulated routing updates from Node B to Node A under various conditions.
 */
@SuppressWarnings("UnstableApiUsage")
public class CcpSenderReceiverTest {

  protected static final String ENCRYPTED_SHH
      = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  private static final AccountId CONNECTOR_A_ACCOUNT = AccountId.of("a");
  private static final AccountId CONNECTOR_B_ACCOUNT = AccountId.of("b");
  private static final AccountId CONNECTOR_C_ACCOUNT = AccountId.of("c");
  private static final AccountId CONNECTOR_D_ACCOUNT = AccountId.of("d");

  private static final InterledgerAddress CONNECTOR_A_ADDRESS =
      InterledgerAddress.of(InterledgerAddressPrefix.TEST1.getValue() + "." + CONNECTOR_A_ACCOUNT.value());

  private static final InterledgerAddress CONNECTOR_B_ADDRESS =
      InterledgerAddress.of(InterledgerAddressPrefix.TEST1.getValue() + "." + CONNECTOR_B_ACCOUNT.value());

  private static final InterledgerAddress CONNECTOR_C_ADDRESS =
      InterledgerAddress.of(InterledgerAddressPrefix.TEST1.getValue() + "." + CONNECTOR_C_ACCOUNT.value());

  private static final InterledgerAddress CONNECTOR_D_ADDRESS =
      InterledgerAddress.of(InterledgerAddressPrefix.TEST1.getValue() + "." + CONNECTOR_D_ACCOUNT.value());

  private static final InterledgerAddressPrefix CONNECTOR_C_PREFIX =
      InterledgerAddressPrefix.of(CONNECTOR_C_ADDRESS.getValue());

  private static final InterledgerAddressPrefix CONNECTOR_D_PREFIX =
      InterledgerAddressPrefix.of(CONNECTOR_D_ADDRESS.getValue());

  private CodecContext codecContext;

  /////////////
  // connectorB
  /////////////
  @Mock
  private AccountSettingsRepository connectorA_AccountSettingsRepository;
  private ConnectorSettings connectorA_ConnectorSettings;
  private SimulatedConnector connectorA;

  /////////////
  // ConnectorB
  /////////////
  @Mock
  private AccountSettingsRepository connectorB_AccountSettingsRepository;
  private ConnectorSettings connectorB_ConnectorSettings;
  private SimulatedConnector connectorB;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    EventBus eventBus = new EventBus();

    this.codecContext = CcpCodecContextFactory.register(InterledgerCodecContextFactory.oer());
    this.connectorA_ConnectorSettings = ImmutableConnectorSettings.builder()
        .globalRoutingSettings(GlobalRoutingSettings.builder().routingSecret(ENCRYPTED_SHH).build())
        .operatorAddress(CONNECTOR_A_ADDRESS)
        .build();
    this.connectorB_ConnectorSettings = ImmutableConnectorSettings.builder()
        .globalRoutingSettings(GlobalRoutingSettings.builder().routingSecret(ENCRYPTED_SHH).build())
        .operatorAddress(CONNECTOR_B_ADDRESS)
        .build();

    final LinkSettings linkSettingsA = LinkSettings.builder()
        .linkType(LinkType.of(IpcRouteHandlingLink.class.getSimpleName()))
        .build();
    final IpcRouteHandlingLink linkRunningOnA = new IpcRouteHandlingLink(
        () -> Optional.of(CONNECTOR_A_ADDRESS), linkSettingsA, codecContext,
        new EventBusConnectionEventEmitter(eventBus)
    );

    {
      // Because this is a simulated ConnectorA, this table is the sole instance for this connector.
      final ForwardingRoutingTable<RouteUpdate> routeUpdateForwardingRoutingTable =
          new InMemoryForwardingRoutingTable();
      final CcpSender ccpSender = new DefaultCcpSender(
          () -> connectorA_ConnectorSettings, CONNECTOR_B_ACCOUNT, linkRunningOnA,
          routeUpdateForwardingRoutingTable, connectorA_AccountSettingsRepository,
          codecContext
      );
      final CcpReceiver ccpReceiver =
          new DefaultCcpReceiver(() -> connectorA_ConnectorSettings, CONNECTOR_B_ACCOUNT, linkRunningOnA, codecContext);
      this.connectorA = new SimulatedConnector(CONNECTOR_A_ADDRESS, linkRunningOnA, ccpSender, ccpReceiver);
    }

    final IpcRouteHandlingLink linkRunningOnB = new IpcRouteHandlingLink(
        () -> Optional.of(CONNECTOR_B_ADDRESS), linkSettingsA, codecContext,
        new EventBusConnectionEventEmitter(eventBus)
    );

    {
      // Because this is a simulated ConnectorB, this table is the sole instance for this connector.
      final ForwardingRoutingTable<RouteUpdate> routeUpdateForwardingRoutingTable =
          new InMemoryForwardingRoutingTable();
      final CcpSender ccpSender = new DefaultCcpSender(
          () -> connectorB_ConnectorSettings, CONNECTOR_A_ACCOUNT, linkRunningOnB, routeUpdateForwardingRoutingTable,
          connectorB_AccountSettingsRepository, codecContext
      );
      final CcpReceiver ccpReceiver =
          new DefaultCcpReceiver(() -> connectorB_ConnectorSettings, CONNECTOR_A_ACCOUNT, linkRunningOnB, codecContext);
      this.connectorB = new SimulatedConnector(CONNECTOR_B_ADDRESS, linkRunningOnB, ccpSender, ccpReceiver);
    }

    // Rather than running HTTP or BTP over web-sockets between ConnectorA and ConnectorB, this arrangement simulates
    // something closer to IPC.
    linkRunningOnA.setConnectors(connectorA, connectorB);
    linkRunningOnB.setConnectors(connectorB, connectorA);

    linkRunningOnA.setLinkId(LinkId.of("linkRunningOnA"));
    linkRunningOnA.connect();
    linkRunningOnB.setLinkId(LinkId.of("linkRunningOnB"));
    linkRunningOnB.connect();
  }

  /**
   * This test does the following to exercise the routing sender/receivers:
   *
   * <pre>
   * 1. NodeB sends a heartbeat to NodeA (expects error).
   * 2. NodeA sends a RouteControlRequest to NodeB, attempting to move NodeB into {@link CcpSyncMode#MODE_SYNC}.
   * NodeB will immediately begin broadcasting routes to NodeA.
   * 3. NodeA sends a heartbeat, which should have no effect.
   * 4. NodeB sends a heartbeat, which should extend the routing table of NodeA.
   * 5. NodeB adds 2 routes, and sends a route update request to NodeA.
   *
   *
   * 3. NodeB moves NodeA to idle.
   * 4. NodeB adds a 3rd route, expects an error.
   * 5. NodeB moves NodeA to sync-mode.
   * 6. NodeB withdraws all routes.
   * 7. NodeB sends a heartbeat.
   * 8. NodeB moves the receiver to idle mode.
   * </pre>
   */
  @Test
  public void testMessageFlow() throws InterruptedException {
    assertRoutingTableIsExpired(connectorA, connectorB);
    assertSyncMode(CcpSyncMode.MODE_IDLE, connectorA, connectorB);
    assertRoutingTableEpoch(0, connectorA, connectorB);

    // 1. Send a Heartbeat from A to B (makes B's routing table not expired)
    // TODO: Make this blockable....
    this.connectorA.ccpSender.sendRouteUpdateRequest();
    assertRoutingTableIsExpired(connectorA);
    // assertRoutingTableIsExpired(connectorB);  // Sometimes B is expired and sometimes its not, depending on timing.
    assertSyncMode(CcpSyncMode.MODE_IDLE, connectorA, connectorB);
    assertRoutingTableEpoch(0, connectorA, connectorB);

    // 2. NodeB sends a RouteControlRequest to move NodeB into SYNC mode.
    this.connectorA.ccpReceiver.sendRouteControl();
    //assertRoutingTableIsNotExpired(connectorA, connectorB);
    assertSyncMode(CcpSyncMode.MODE_IDLE, connectorA);
    assertSyncMode(CcpSyncMode.MODE_SYNC, connectorB);
    assertRoutingTableEpoch(0, connectorA, connectorB);

    // 3. NodeA sends a Heartbeat (expect NodeB's routing table expiry to be extended).
    this.connectorB.ccpSender.sendRouteUpdateRequest();
    //assertRoutingTableIsNotExpired(connectorA, connectorB);
    assertSyncMode(CcpSyncMode.MODE_SYNC, connectorB);
    assertSyncMode(CcpSyncMode.MODE_IDLE, connectorA);
    assertRoutingTableEpoch(0, connectorA, connectorB);

    // 4. NodeB sends a Heartbeat, which has no effect.
    this.connectorB.ccpSender.sendRouteUpdateRequest();
    //assertRoutingTableIsNotExpired(connectorA, connectorB);
    assertSyncMode(CcpSyncMode.MODE_IDLE, connectorA);
    assertSyncMode(CcpSyncMode.MODE_SYNC, connectorB);
    assertRoutingTableEpoch(0, connectorA, connectorB);

    //////////////////////////
    // 5. NodeB adds 2 routes.
    //////////////////////////

    // The Route to C
    ((DefaultCcpSender) this.connectorB.ccpSender).getForwardingRoutingTable().addRoute(
        ImmutableRouteUpdate.builder()
            .epoch(0)
            .route(
                ImmutableRoute.builder()
                    .routePrefix(CONNECTOR_C_PREFIX)
                    .addPath(CONNECTOR_C_ADDRESS)
                    .nextHopAccountId(CONNECTOR_C_ACCOUNT)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build()
            )
            .routePrefix(CONNECTOR_C_PREFIX)
            .build()
    );

    // The Route to D through C.
    ((DefaultCcpSender) this.connectorB.ccpSender).getForwardingRoutingTable().addRoute(
        ImmutableRouteUpdate.builder()
            .epoch(0)
            .route(
                ImmutableRoute.builder()
                    .routePrefix(CONNECTOR_D_PREFIX)
                    .addPath(CONNECTOR_C_ADDRESS, CONNECTOR_D_ADDRESS)
                    .nextHopAccountId(CONNECTOR_C_ACCOUNT)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build()
            )
            .routePrefix(CONNECTOR_D_PREFIX)
            .build()
    );

    // Connector B should have these routes....
    assertHasRouteForPrefix(CONNECTOR_C_PREFIX, connectorB);
    assertHasRouteForPrefix(CONNECTOR_D_PREFIX, connectorB);

    // ConnectorB sends the route updates to Connectors A and C.
    this.connectorB.ccpSender.sendRouteUpdateRequest();
    Thread.sleep(1000); // Wait for it!
    assertRoutingTableIsNotExpired(connectorA, connectorB);
    assertSyncMode(CcpSyncMode.MODE_IDLE, connectorA);
    assertSyncMode(CcpSyncMode.MODE_SYNC, connectorB);
    assertRoutingTableEpoch(0, connectorA);
    assertRoutingTableEpoch(0, connectorB);

    // TODO: FIXME with a real connector!!! But where should that come from?
    //assertHasRouteForPrefix(CONNECTOR_B_PREFIX, connectorA); // ConnA has a route to B.
    //assertHasRouteForPrefix(CONNECTOR_C_PREFIX, connectorA); // ConnA has a route to C.
    //assertHasRouteForPrefix(CONNECTOR_C_PREFIX, connectorA); // ConnA has a route to D.
  }

  private void assertRoutingTableEpoch(final int expectedEpoch, final SimulatedConnector... connectors) {
    Stream.of(connectors).forEach(connector ->
        assertThat(((DefaultCcpSender) connector.ccpSender).getForwardingRoutingTable().getCurrentEpoch(),
            is(expectedEpoch))
    );
  }

  private void assertSyncMode(final CcpSyncMode syncMode, final SimulatedConnector... connectors) {
    Stream.of(connectors).forEach(connector ->
        assertThat(((DefaultCcpSender) connector.ccpSender).getSyncMode(), is(syncMode))
    );
  }

  private void assertRoutingTableIsNotExpired(final SimulatedConnector... connectors) {
    Stream.of(connectors).forEach(connector ->
        assertThat(
            ((DefaultCcpReceiver) connector.ccpReceiver).getRoutingTableExpiry().isAfter(Instant.now()),
            is(true))
    );
  }

  private void assertRoutingTableIsExpired(final SimulatedConnector... connectors) {
    Stream.of(connectors).forEach(connector ->
        assertThat(
            ((DefaultCcpReceiver) connector.ccpReceiver).getRoutingTableExpiry()
                .isBefore(Instant.now().plusMillis(1)),
            is(true))
    );
  }

  private void assertHasRouteForPrefix(final InterledgerAddressPrefix prefix, final SimulatedConnector... connectors) {
    Stream.of(connectors).forEach(connector ->
        assertThat(
            ((DefaultCcpSender) connector.ccpSender)
                .getForwardingRoutingTable().getRouteByPrefix(prefix).isPresent(),
            is(true)
        )
    );
  }

  /**
   * Models a very stripped-down, simulated, Ilpv4 Connector that has a single lpi2 associated with a {@link CcpSender}
   * and a {@link CcpReceiver}.
   */
  private static class SimulatedConnector {

    private final InterledgerAddress address;
    private final IpcRouteHandlingLink link;

    // Holds links for simulated nodes for C and D in this test...
    private final List<IpcRouteHandlingLink> mockLinks;

    private final CcpSender ccpSender;
    private final CcpReceiver ccpReceiver;

    private SimulatedConnector(
        final InterledgerAddress address,
        final IpcRouteHandlingLink link,
        final CcpSender ccpSender,
        final CcpReceiver ccpReceiver,
        final IpcRouteHandlingLink... mockLinks
    ) {
      this.address = Objects.requireNonNull(address);
      this.link = Objects.requireNonNull(link);
      this.ccpSender = Objects.requireNonNull(ccpSender);
      this.ccpReceiver = Objects.requireNonNull(ccpReceiver);

      this.mockLinks = Lists.newArrayList(mockLinks);
    }

    public IpcRouteHandlingLink getLink() {
      return link;
    }
  }

  /**
   * A lpi2 that directly connects ¬two Connectors via Inter-process Communication (IPC), for testing of route control
   * messages. Each Connector in a bilateral relationship will have an instance of this Link.
   */
  private static class IpcRouteHandlingLink extends AbstractStatefulLink<LinkSettings> implements Link<LinkSettings> {

    private static final String PEER_ROUTE = "peer.route";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private SimulatedConnector localConnector;
    private SimulatedConnector remoteConnector;

    private IpcRouteHandlingLink(
        final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
        final LinkSettings linkSettings,
        final CodecContext codecContext,
        final LinkConnectionEventEmitter linkConnectionEventEmitter
    ) {
      // TODO: this will become safe when https://github.com/sappenin/java-ilpv4-connector/issues/302 is merged.
      super(() -> operatorAddressSupplier.get().get(), linkSettings, linkConnectionEventEmitter);
      Objects.requireNonNull(codecContext);

      /////////////////////////////
      // The stuff to do on an incoming prepare packet...
      /////////////////////////////
      this.registerLinkHandler((sourcePreparePacket) -> {

        if (sourcePreparePacket.getDestination().startsWith(PEER_ROUTE)) {
          switch (sourcePreparePacket.getDestination().getValue()) {
            case CcpConstants.CCP_CONTROL_DESTINATION: {
              final CcpRouteControlRequest routeControlRequest;
              try {
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourcePreparePacket.getData());
                routeControlRequest = codecContext.read(CcpRouteControlRequest.class, inputStream);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              logger.debug(
                  "Received route control message. sender={}, tableId={} epoch={} features={}",
                  operatorAddressSupplier,
                  routeControlRequest.lastKnownRoutingTableId(), routeControlRequest.lastKnownEpoch(),
                  routeControlRequest.features()
              );

              // Normally, we would consult the AccountManager to getEntry the lpi2 for the sourceAccount. However, for
              // this test, it's hard-coded, so just use the sender directly...
              this.localConnector.ccpSender.handleRouteControlRequest(routeControlRequest);

              // If no exception, then return a fulfill response...
              return InterledgerFulfillPacket.builder()
                  .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
                  .build();
            }

            case CcpConstants.CCP_UPDATE_DESTINATION: {
              final CcpRouteUpdateRequest routeUpdateRequest;
              try {
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourcePreparePacket.getData());
                routeUpdateRequest = codecContext.read(CcpRouteUpdateRequest.class, inputStream);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              logger.debug(
                  "Received routes. sender={} speaker={} currentEpoch={} fromEpoch={} toEpoch={} newRoutes={} " +
                      "withdrawnRoutes={}",
                  operatorAddressSupplier,
                  routeUpdateRequest.speaker(), routeUpdateRequest.currentEpochIndex(),
                  routeUpdateRequest.fromEpochIndex(), routeUpdateRequest.toEpochIndex(),
                  routeUpdateRequest.newRoutes().size(),
                  routeUpdateRequest.withdrawnRoutePrefixes().size()
              );

              // Normally, we would consult the AccountManager to getEntry the lpi2 for the sourceAccount. However, for
              // this test, it's hard-coded, so just use the sender directly...
              final List<InterledgerAddressPrefix> changedPrefixes =
                  this.localConnector.ccpReceiver.handleRouteUpdateRequest(routeUpdateRequest);

              // TODO: In the real connector, we would update the local routing table via the following two calls in
              // route-broadcaster.js->updatePrefix(prefix:String) {
              // const newBest = this.getCurrentBestPeerRouteForPrefix(prefix)
              // return this.updateLocalRoute(prefix, newBest)
              // }
              // But in this test we don't do that...

              // If no exception, then return a fulfill response...
              return InterledgerFulfillPacket.builder()
                  .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
                  .build();
            }

            default:
              throw new Error("Unrecognized ccp message. destination=" + sourcePreparePacket.getDestination());
          }
        }
        throw new RuntimeException("Unhandled Destination: " + sourcePreparePacket.getDestination());
      });

      //      this.registerMoneyHandler((amount) -> {
      //        // No-op.
      //      });
    }

    @Override
    public CompletableFuture<Void> doConnect() {
      // No-op
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> doDisconnect() {
      // No-op
      return CompletableFuture.completedFuture(null);
    }

    /**
     * Perform the logic of sending a packet to a remote peer.
     *
     * @param preparePacket
     */
    @Override
    public InterledgerResponsePacket sendPacket(InterledgerPreparePacket preparePacket)
        throws InterledgerProtocolException {
      // For simulation purposes, we simply reach through into the other connector directly and place the
      // preparePacket into it.
      return this.remoteConnector.getLink().getLinkHandler().get().handleIncomingPacket(preparePacket);
    }

    public void setConnectors(final SimulatedConnector localConnector, final SimulatedConnector remoteConnector) {
      if (this.localConnector == null) {
        this.localConnector = Objects.requireNonNull(localConnector);
      } else {
        throw new RuntimeException("Can't assign the localConnector more than once!");
      }
      if (this.remoteConnector == null) {
        this.remoteConnector = Objects.requireNonNull(remoteConnector);
      } else {
        throw new RuntimeException("Can't assign the remoteConnector more than once!");
      }
    }
  }
}
