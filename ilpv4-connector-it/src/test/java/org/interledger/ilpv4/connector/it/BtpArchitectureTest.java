package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple test to verify that two nodes can speak BTP to each other over Websockets.
 */
// TODO: Rename this to align with Topology naming convention...
public class BtpArchitectureTest extends AbstractArchitectureTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(BtpArchitectureTest.class);
  private static Topology topology = null; //BtpArchitectures.twoPeerGraph();

  //
  //  @BeforeClass
  //  public static void setup() {
  //    LOGGER.info("Starting test topology `{}`...", "twoPeerGraph");
  //    topology.start();
  //
  //    //////////////////
  //    // Alice Routing Table
  //    //////////////////
  //    {
  //      // Configure Routing...
  //      final PaymentRouter<Route> aliceRoutingTable = toConnectorNode(topology, ALICE_AT_CONNIE).getIlpConnector().getPaymentRouter();
  //
  //      // Payments to Bob should route to Bob...
  //      aliceRoutingTable.getRoutingTable().addRoute(InterledgerAddressPrefix.from(BOB_AT_CONNIE), ImmutableRoute.builder()
  //        // This route handles all `test.bob` prefixes...
  //        .routePrefix(InterledgerAddressPrefix.from(BOB_AT_CONNIE))
  //        .nextHopAccount(BOB_AT_CONNIE.with(USD))
  //        .addPath(BOB_AT_CONNIE)
  //        .expiresAt(Instant.MAX)
  //        .build()
  //      );
  //      // By default, all payments are handled locally...
  //      aliceRoutingTable.setDefaultRoute(ALICE_AT_CONNIE);
  //    }
  //
  //    //////////////////
  //    // Bob Routing Table
  //    //////////////////
  //    {
  //      // Configure Routing...
  //      final PaymentRouter<Route> bobRoutingTable = toConnectorNode(topology, BOB_AT_CONNIE).getIlpConnector().getPaymentRouter();
  //
  //      // Payments to Bob should route to Bob...
  //      bobRoutingTable.getRoutingTable().addRoute(InterledgerAddressPrefix.from(ALICE_AT_CONNIE), ImmutableRoute.builder()
  //        // This route handles all `test.bob` prefixes...
  //        .routePrefix(InterledgerAddressPrefix.from(ALICE_AT_CONNIE))
  //        .nextHopAccount(ALICE_AT_CONNIE.with(USD))
  //        .addPath(ALICE_AT_CONNIE)
  //        .expiresAt(Instant.MAX)
  //        .build()
  //      );
  //      // By default, all payments are handled locally...
  //      bobRoutingTable.setDefaultRoute(BOB_AT_CONNIE);
  //    }
  //
  //    LOGGER.info("Test topology `{}` started!", "twoPeerGraph");
  //  }
  //
  //  @AfterClass
  //  public static void shutdown() {
  //    LOGGER.info("Stopping test topology `{}`...", "twoPeerGraph");
  //    topology.stop();
  //    LOGGER.info("Test topology `{}` stopped!", "twoPeerGraph");
  //  }
  //
  //  @Test
  //  public void testAliceNodeSettings() {
  //    final ILPv4Connector aliceConnector = getIlpConnectorFromGraph(ALICE_AT_CONNIE);
  //
  //    // By default, the settings have no accounts, but accounts are added after the server starts...
  //    assertThat(aliceConnector.getConnectorSettings().getAccount().size(), is(0));
  //    assertThat(aliceConnector.getAccountManager().getAllAccounts().count(), is(1L));
  //    assertThat(aliceConnector.getAccountManager().getOrCreatePlugin(BOB_AT_CONNIE).isConnected(), is(true));
  //
  //    // Bob's account, from Alice's perspective...
  //    final AccountSettings bobAccount = aliceConnector.getAccountManager().getAccount(BOB_AT_CONNIE)
  //      .orElseThrow(() -> new RuntimeException("Expected 1 account for Bob!"));
  //    assertThat(bobAccount.getInterledgerAddress(), is(BOB_AT_CONNIE));
  //    assertThat(bobAccount.getRelationship(), is(AccountSettings.IlpRelationship.CHILD));
  //    assertThat(bobAccount.getAssetCode(), is("USD"));
  //  }
  //
  //  @Test
  //  public void testBobNodeSettings() {
  //    final ILPv4Connector bobConnector = getIlpConnectorFromGraph(BOB_AT_CONNIE);
  //
  //    // By default, the settings have no accounts, but accounts are added after the server starts...
  //    assertThat(bobConnector.getConnectorSettings().getAccount().size(), is(0));
  //    assertThat(bobConnector.getAccountManager().getAllAccounts().count(), is(1L));
  //    assertThat(bobConnector.getAccountManager().getOrCreatePlugin(ALICE_AT_CONNIE).isConnected(), is(true));
  //
  //    // Alice's account, from Bob's perspective...
  //    final AccountSettings aliceAccount = bobConnector.getAccountManager().getAccount(ALICE_AT_CONNIE)
  //      .orElseThrow(() -> new RuntimeException("Expected 1 account for Bob!"));
  //    assertThat(aliceAccount.getInterledgerAddress(), is(ALICE_AT_CONNIE));
  //    assertThat(aliceAccount.getRelationship(), is(AccountSettings.IlpRelationship.PARENT));
  //    assertThat(aliceAccount.getAssetCode(), is("USD"));
  //  }
  //
  //  @Test
  //  public void alicePaysBob() throws Throwable {
  //    final ILPv4Connector aliceConnector = getIlpConnectorFromGraph(ALICE_AT_CONNIE);
  //    //final ILPv4Connector bobConnector = getIlpConnectorFromGraph(BOB_AT_CONNIE);
  //
  //    final InterledgerFulfillment fulfillment = InterledgerFulfillment.of("01234567890123456789012345678901".getBytes());
  //    final InterledgerPreparePacket preparePacket =
  //      InterledgerPreparePacket.builder()
  //        .amount(BigInteger.TEN)
  //        .expiresAt(Instant.now().plusSeconds(60))
  //        .destination(BOB_AT_CONNIE.with("usd"))
  //        .executionCondition(fulfillment.getCondition())
  //        .build();
  //
  //    try {
  //      final InterledgerFulfillPacket fulfillPacket =
  //        aliceConnector.getAccountManager().getOrCreatePlugin(BOB_AT_CONNIE)
  //          .routeData(preparePacket).get();
  //      assertThat(fulfillPacket.getFulfillment(), is(fulfillment));
  //    } catch (ExecutionException e) {
  //      throw e.getCause();
  //    }
  //  }
  //

  @Override
  protected Topology getGraph() {
    return topology;
  }
}
