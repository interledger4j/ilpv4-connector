package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteUpdateRequest;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpSyncMode;
import com.sappenin.interledger.ilpv4.connector.ccp.codecs.CcpCodecs;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.ImmutableLinkSettings;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
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
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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
 * This harness exercises simulated routing updates from Node B to Node A under carious conditions.
 */
public class CcpSenderReceiverTest {

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

  //  private static final InterledgerAddressPrefix CONNECTOR_A_PREFIX =
  //    InterledgerAddressPrefix.of(CONNECTOR_A_ADDRESS.getValue());
  //  private static final InterledgerAddressPrefix CONNECTOR_B_PREFIX =
  //    InterledgerAddressPrefix.of(CONNECTOR_B_ADDRESS.getValue());

  private static final InterledgerAddressPrefix CONNECTOR_C_PREFIX =
    InterledgerAddressPrefix.of(CONNECTOR_C_ADDRESS.getValue());
  private static final InterledgerAddressPrefix CONNECTOR_D_PREFIX =
    InterledgerAddressPrefix.of(CONNECTOR_D_ADDRESS.getValue());

  private CodecContext codecContext;

  /////////////
  // connectorB
  /////////////
  @Mock
  private AccountManager connectorAAccountManagerMock;
  private ConnectorSettings connectorA_ConnectorSettings;
  private SimulatedConnector connectorA;

  /////////////
  // ConnectorB
  /////////////
  @Mock
  private AccountManager connectorB_AccountManagerMock;
  private ConnectorSettings connectorB_ConnectorSettings;
  private SimulatedConnector connectorB;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.codecContext = CcpCodecs.register(InterledgerCodecContextFactory.oer());
    this.connectorA_ConnectorSettings = ImmutableConnectorSettings.builder()
      .globalRoutingSettings(GlobalRoutingSettings.builder().routingSecret("shh").build())
      .operatorAddress(CONNECTOR_A_ADDRESS)
      .build();
    this.connectorB_ConnectorSettings = ImmutableConnectorSettings.builder()
      .globalRoutingSettings(GlobalRoutingSettings.builder().routingSecret("shh").build())
      .operatorAddress(CONNECTOR_B_ADDRESS)
      .build();

    final LinkSettings linkSettingsA = ImmutableLinkSettings.builder()
      .linkType(LinkType.of(IpcRouteHandlingLink.class.getSimpleName()))
      .operatorAddress(CONNECTOR_A_ADDRESS)
      .build();
    final IpcRouteHandlingLink linkRunningOnA = new IpcRouteHandlingLink(linkSettingsA, codecContext);

    {
      final ForwardingRoutingTable<RouteUpdate> routeUpdateForwardingRoutingTable =
        new InMemoryRouteUpdateForwardRoutingTable();
      final ForwardingRoutingTable<IncomingRoute> incomingRouteForwardingRoutingTable =
        new InMemoryIncomingRouteForwardRoutingTable();
      final CcpSender ccpSender = new DefaultCcpSender(
        () -> connectorA_ConnectorSettings, CONNECTOR_B_ACCOUNT, linkRunningOnA,
        routeUpdateForwardingRoutingTable, connectorAAccountManagerMock, codecContext
      );
      final CcpReceiver ccpReceiver =
        new DefaultCcpReceiver(() -> connectorA_ConnectorSettings, CONNECTOR_B_ACCOUNT, linkRunningOnA,
          incomingRouteForwardingRoutingTable,
          codecContext
        );

      this.connectorA = new SimulatedConnector(CONNECTOR_A_ADDRESS, linkRunningOnA, ccpSender, ccpReceiver);
    }

    final IpcRouteHandlingLink linkRunningOnB = new IpcRouteHandlingLink(linkSettingsA, codecContext);
    {
      final ForwardingRoutingTable<RouteUpdate> routeUpdateForwardingRoutingTable =
        new InMemoryRouteUpdateForwardRoutingTable();
      final ForwardingRoutingTable<IncomingRoute> incomingRouteForwardingRoutingTable =
        new InMemoryIncomingRouteForwardRoutingTable();
      final CcpSender ccpSender = new DefaultCcpSender(
        () -> connectorB_ConnectorSettings,
        CONNECTOR_A_ACCOUNT, linkRunningOnB, routeUpdateForwardingRoutingTable,
        connectorB_AccountManagerMock, codecContext
      );
      final CcpReceiver ccpReceiver =
        new DefaultCcpReceiver(() -> connectorB_ConnectorSettings, CONNECTOR_A_ACCOUNT, linkRunningOnB,
          incomingRouteForwardingRoutingTable,
          codecContext
        );

      this.connectorB = new SimulatedConnector(CONNECTOR_B_ADDRESS, linkRunningOnB, ccpSender, ccpReceiver);
    }

    // Rather than running HTTP or BTP over web-sockets between ConnectorA and ConnectorB, this arrangement simulates
    // something closer to IPC.
    linkRunningOnA.setConnectors(connectorA, connectorB);
    linkRunningOnB.setConnectors(connectorB, connectorA);

    linkRunningOnA.connect();
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
   * 5. NodeB adds 2 routes, and sends a getRoute update request to NodeA.
   *
   *
   * 3. NodeB moves NodeA to idle.
   * 4. NodeB adds a 3rd getRoute, expects an error.
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
    // TODO: Make this blockable....
    this.connectorB.ccpSender.sendRouteUpdateRequest();
    //Thread.sleep(100);
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
      CONNECTOR_C_PREFIX, ImmutableRouteUpdate.builder()
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
      CONNECTOR_C_PREFIX, ImmutableRouteUpdate.builder()
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
          .getForwardingRoutingTable().getRouteForPrefix(prefix).isPresent(),
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
      final InterledgerAddress address, final IpcRouteHandlingLink link,
      final CcpSender ccpSender, final CcpReceiver ccpReceiver,
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

    // The address of this connector...
    public InterledgerAddress getAddress() {
      return address;
    }
  }

  /**
   * A lpi2 that directly connects ¬two Connectors via Inter-process Communication (IPC), for testing of getRoute
   * control messages. Each Connector in a bilateral relationship will have an instance of this Link.
   */
  private static class IpcRouteHandlingLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

    private static final String PEER_ROUTE = "peer.route";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // The receiver on the other side of this simulated lpi2 relationship...
    private final CodecContext codecContext;

    private SimulatedConnector localConnector;
    private SimulatedConnector remoteConnector;

    private IpcRouteHandlingLink(final LinkSettings linkSettings, final CodecContext codecContext) {
      super(linkSettings);
      this.codecContext = Objects.requireNonNull(codecContext);

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

              logger
                .debug("Received getRoute control message. sender={}, tableId={} getEpoch={} features={}",
                  linkSettings.getOperatorAddress(),
                  routeControlRequest.lastKnownRoutingTableId(), routeControlRequest.lastKnownEpoch(),
                  routeControlRequest.features());

              // Normally, we would consult the AccountManager to getEntry the lpi2 for the sourceAccount. However, for
              // this test, it's hard-coded, so just use the sender directly...
              this.localConnector.ccpSender.handleRouteControlRequest(routeControlRequest);

              // If no exception, then return a fulfill response...
              return Optional.of(
                InterledgerFulfillPacket.builder()
                  .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
                  .build()
              );
            }

            case CcpConstants.CCP_UPDATE_DESTINATION: {
              final CcpRouteUpdateRequest routeUpdateRequest;
              try {
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourcePreparePacket.getData());
                routeUpdateRequest = codecContext.read(CcpRouteUpdateRequest.class, inputStream);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              logger
                .debug("Received routes. sender={} speaker={} currentEpoch={} fromEpoch={} toEpoch={} newRoutes={} " +
                    "withdrawnRoutes={}",
                  linkSettings.getOperatorAddress(),
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
              // getRoute-broadcaster.js->updatePrefix(prefix:String) {
              // const newBest = this.getBestPeerRouteForPrefix(prefix)
              // return this.updateLocalRoute(prefix, newBest)
              // }
              // But in this test we don't do that...

              // If no exception, then return a fulfill response...
              return Optional.of(
                InterledgerFulfillPacket.builder()
                  .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
                  .build()
              );
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
    public Optional<InterledgerResponsePacket> sendPacket(InterledgerPreparePacket preparePacket)
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