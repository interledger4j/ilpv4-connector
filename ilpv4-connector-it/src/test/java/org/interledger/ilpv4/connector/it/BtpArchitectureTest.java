package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple test to verify that two nodes can speak BTP to each other over Websockets.
 */
// TODO: Rename this to align with Topology naming convention...
public class BtpArchitectureTest extends AbstractArchitectureTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(BtpArchitectureTest.class);
  private static Graph graph = null; //BtpArchitectures.twoPeerGraph();

  //
  //  @BeforeClass
  //  public static void setup() {
  //    LOGGER.info("Starting test graph `{}`...", "twoPeerGraph");
  //    graph.start();
  //
  //    //////////////////
  //    // Alice Routing Table
  //    //////////////////
  //    {
  //      // Configure Routing...
  //      final PaymentRouter<Route> aliceRoutingTable = toConnectorNode(graph, ALICE).getIlpConnector().getPaymentRouter();
  //
  //      // Payments to Bob should route to Bob...
  //      aliceRoutingTable.getRoutingTable().addRoute(InterledgerAddressPrefix.from(BOB), ImmutableRoute.builder()
  //        // This route handles all `test.bob` prefixes...
  //        .routePrefix(InterledgerAddressPrefix.from(BOB))
  //        .nextHopAccount(BOB.with(USD))
  //        .addPath(BOB)
  //        .expiresAt(Instant.MAX)
  //        .build()
  //      );
  //      // By default, all payments are handled locally...
  //      aliceRoutingTable.setDefaultRoute(ALICE);
  //    }
  //
  //    //////////////////
  //    // Bob Routing Table
  //    //////////////////
  //    {
  //      // Configure Routing...
  //      final PaymentRouter<Route> bobRoutingTable = toConnectorNode(graph, BOB).getIlpConnector().getPaymentRouter();
  //
  //      // Payments to Bob should route to Bob...
  //      bobRoutingTable.getRoutingTable().addRoute(InterledgerAddressPrefix.from(ALICE), ImmutableRoute.builder()
  //        // This route handles all `test.bob` prefixes...
  //        .routePrefix(InterledgerAddressPrefix.from(ALICE))
  //        .nextHopAccount(ALICE.with(USD))
  //        .addPath(ALICE)
  //        .expiresAt(Instant.MAX)
  //        .build()
  //      );
  //      // By default, all payments are handled locally...
  //      bobRoutingTable.setDefaultRoute(BOB);
  //    }
  //
  //    LOGGER.info("Test graph `{}` started!", "twoPeerGraph");
  //  }
  //
  //  @AfterClass
  //  public static void shutdown() {
  //    LOGGER.info("Stopping test graph `{}`...", "twoPeerGraph");
  //    graph.stop();
  //    LOGGER.info("Test graph `{}` stopped!", "twoPeerGraph");
  //  }
  //
  //  @Test
  //  public void testAliceNodeSettings() {
  //    final ILPv4Connector aliceConnector = getIlpConnectorFromGraph(ALICE);
  //
  //    // By default, the settings have no accounts, but accounts are added after the server starts...
  //    assertThat(aliceConnector.getConnectorSettings().getAccountSettings().size(), is(0));
  //    assertThat(aliceConnector.getAccountManager().getAllAccountSettings().count(), is(1L));
  //    assertThat(aliceConnector.getAccountManager().getOrCreatePlugin(BOB).isConnected(), is(true));
  //
  //    // Bob's account, from Alice's perspective...
  //    final AccountSettings bobAccount = aliceConnector.getAccountManager().getAccountSettings(BOB)
  //      .orElseThrow(() -> new RuntimeException("Expected 1 account for Bob!"));
  //    assertThat(bobAccount.getInterledgerAddress(), is(BOB));
  //    assertThat(bobAccount.getRelationship(), is(AccountSettings.IlpRelationship.CHILD));
  //    assertThat(bobAccount.getAssetCode(), is("USD"));
  //  }
  //
  //  @Test
  //  public void testBobNodeSettings() {
  //    final ILPv4Connector bobConnector = getIlpConnectorFromGraph(BOB);
  //
  //    // By default, the settings have no accounts, but accounts are added after the server starts...
  //    assertThat(bobConnector.getConnectorSettings().getAccountSettings().size(), is(0));
  //    assertThat(bobConnector.getAccountManager().getAllAccountSettings().count(), is(1L));
  //    assertThat(bobConnector.getAccountManager().getOrCreatePlugin(ALICE).isConnected(), is(true));
  //
  //    // Alice's account, from Bob's perspective...
  //    final AccountSettings aliceAccount = bobConnector.getAccountManager().getAccountSettings(ALICE)
  //      .orElseThrow(() -> new RuntimeException("Expected 1 account for Bob!"));
  //    assertThat(aliceAccount.getInterledgerAddress(), is(ALICE));
  //    assertThat(aliceAccount.getRelationship(), is(AccountSettings.IlpRelationship.PARENT));
  //    assertThat(aliceAccount.getAssetCode(), is("USD"));
  //  }
  //
  //  @Test
  //  public void alicePaysBob() throws Throwable {
  //    final ILPv4Connector aliceConnector = getIlpConnectorFromGraph(ALICE);
  //    //final ILPv4Connector bobConnector = getIlpConnectorFromGraph(BOB);
  //
  //    final InterledgerFulfillment fulfillment = InterledgerFulfillment.of("01234567890123456789012345678901".getBytes());
  //    final InterledgerPreparePacket preparePacket =
  //      InterledgerPreparePacket.builder()
  //        .amount(BigInteger.TEN)
  //        .expiresAt(Instant.now().plusSeconds(60))
  //        .destination(BOB.with("usd"))
  //        .executionCondition(fulfillment.getCondition())
  //        .build();
  //
  //    try {
  //      final InterledgerFulfillPacket fulfillPacket =
  //        aliceConnector.getAccountManager().getOrCreatePlugin(BOB)
  //          .sendData(preparePacket).get();
  //      assertThat(fulfillPacket.getFulfillment(), is(fulfillment));
  //    } catch (ExecutionException e) {
  //      throw e.getCause();
  //    }
  //  }
  //

  @Override
  protected Graph getGraph() {
    return graph;
  }
}
