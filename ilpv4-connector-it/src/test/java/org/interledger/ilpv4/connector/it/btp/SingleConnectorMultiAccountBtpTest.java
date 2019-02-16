package org.interledger.ilpv4.connector.it.btp;

import org.interledger.ilpv4.ILPv4Receiver;
import org.interledger.ilpv4.ILPv4Sender;

/**
 * Tests to verify that a single connector can route data and money between two child peers. In this test, value is
 * transferred both from Alice->Connector->Bob, and in the opposite direction. Thus, both Alice and Bob sometimes play
 * the role of sender (via {@link ILPv4Sender}), and sometimes play the role of receiver (via {@link ILPv4Receiver}.
 */
public class SingleConnectorMultiAccountBtpTest {

//  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorMultiAccountBtpTest.class);
//  private static Topology topology = SingleConnectorMultiAccountBtpTopology.init();
//  private final int TIMEOUT = 2;
//
//  @BeforeClass
//  public static void setup() {
//    System.setProperty("spring.profiles.active", BTP_MULTI_ACCOUNT_CONNECTOR);
//
//    LOGGER.info("Starting test topology `{}`...", "SingleConnectorMultiAccountBtpTopology");
//    topology.start();
//    LOGGER.info("Test topology `{}` started!", "SingleConnectorMultiAccountBtpTopology");
//  }
//
//  @AfterClass
//  public static void shutdown() {
//    LOGGER.info("Stopping test topology `{}`...", "SingleConnectorMultiAccountBtpTopology");
//    topology.stop();
//    LOGGER.info("Test topology `{}` stopped!", "SingleConnectorMultiAccountBtpTopology");
//  }
//
//  @Test
//  public void testAliceNodeSettings() {
//    final Plugin client = getPluginNodeFromGraph(ALICE_ADDRESS).getPlugin(ALICE_AT_CONNIE);
//    assertThat(client.getPluginSettings().getLocalNodeAddress(), is(ALICE_AT_CONNIE));
//    assertThat(client.isConnected(), is(true));
//  }
//
//  //  @Test
//  //  public void testConnieNodeSettings() {
//  //    final Plugin alicePluginInConnie = getPluginNodeFromGraph(CONNIE).getPlugin(ALICE_AT_CONNIE);
//  //    assertThat(alicePluginInConnie.getPluginSettings().getLocalNodeAddress(), is(CONNIE));
//  //    assertThat(alicePluginInConnie.getPluginSettings().getAccountAddress(), is(ALICE_AT_CONNIE));
//  //
//  //    final Plugin bobPluginInConnie = getPluginNodeFromGraph(CONNIE).getPlugin(BOB_AT_CONNIE);
//  //    assertThat(bobPluginInConnie.getPluginSettings().getLocalNodeAddress(), is(CONNIE));
//  //    assertThat(bobPluginInConnie.getPluginSettings().getAccountAddress(), is(BOB_AT_CONNIE));
//  //  }
//  //
//  //  @Test
//  //  public void testBobNodeSettings() {
//  //    final Plugin client = getPluginNodeFromGraph(BOB_AT_CONNIE).getPlugin(BOB_AT_CONNIE);
//  //    assertThat(client.getPluginSettings().getLocalNodeAddress(), is(BOB_AT_CONNIE));
//  //    assertThat(client.isConnected(), is(true));
//  //  }
//  //
//  //  @Test
//  //  public void testAlicePingsAlice() throws InterruptedException, ExecutionException, TimeoutException {
//  //    this.testPing(ALICE_ADDRESS, ALICE_ADDRESS);
//  //  }
//  //
//  //  @Test
//  //  public void testAlicePingsAliceAtConnie() throws InterruptedException, ExecutionException, TimeoutException {
//  //    this.testPing(ALICE_ADDRESS, ALICE_AT_CONNIE);
//  //  }
//  //
//  //  @Test
//  //  public void testAlicePingsConnie() throws InterruptedException, ExecutionException, TimeoutException {
//  //    this.testPing(ALICE_ADDRESS, CONNIE);
//  //  }
//  //
//  //  @Test
//  //  public void testAlicePingsBob() throws InterruptedException, ExecutionException, TimeoutException {
//  //    this.testPing(ALICE_ADDRESS, BOB_AT_CONNIE);
//  //  }
//
//  /////////////////
//  // Helper Methods
//  /////////////////
//
//  /**
//   * Helper method to obtain an instance of {@link ILPv4Connector} from the topology, based upon its Interledger Address.
//   *
//   * @param interledgerAddress
//   *
//   * @return
//   */
//  private ILPv4Node getILPv4NodeFromGraph(final InterledgerAddress interledgerAddress) {
//    Objects.requireNonNull(interledgerAddress);
//    return (ILPv4Node) topology.getNode(interledgerAddress).getContentObject();
//  }
//
//  /**
//   * Helper method to obtain an instance of {@link PluginNode} from the topology, based upon its Interledger Address.
//   *
//   * @param interledgerAddress The unique key of the node to return.
//   *
//   * @return
//   */
//  private PluginNode<?> getPluginNodeFromGraph(final InterledgerAddress interledgerAddress) {
//    Objects.requireNonNull(interledgerAddress);
//    return (PluginNode) topology.getNode(interledgerAddress);
//  }
//
//  //  /**
//  //   * Helper method to obtain an instance of {@link PluginNode} from the topology, based upon its Interledger Address.
//  //   *
//  //   * @param interledgerAddress The unique key of the node to return.
//  //   *
//  //   * @return
//  //   */
//  //  private BilateralSender getClientSenderFromGraph(final InterledgerAddress interledgerAddress) {
//  //    Objects.requireNonNull(interledgerAddress);
//  //
//  //    final BilateralConnection connection = ((BtpSingleAccountClientNode) topology.getNode(ALICE_ADDRESS)).getContentObject();
//  //    final AbstractBilateralComboMux comboMux = (AbstractBilateralComboMux) connection.getBilateralSenderMux();
//  //
//  //    return comboMux.getBilateralSender(ALICE_AT_CONNIE).get();
//  //  }
//
//  /**
//   * Helper method to testing ping functionality.
//   *
//   * @param senderNodeAddress  The {@link InterledgerAddress} for the node initiating the ILP ping.
//   * @param destinationAddress The {@link InterledgerAddress} to ping.
//   *
//   * @throws InterruptedException
//   * @throws ExecutionException
//   * @throws TimeoutException
//   */
//  private void testPing(
//    final InterledgerAddress senderNodeAddress,
//    final InterledgerAddress senderAddress, final InterledgerAddress destinationAddress
//  ) throws InterruptedException, ExecutionException, TimeoutException {
//
//    Objects.requireNonNull(senderNodeAddress);
//    Objects.requireNonNull(destinationAddress);
//
//    final PluginNode<?> senderNode = getPluginNodeFromGraph(senderNodeAddress);
//    final Plugin senderPlugin = senderNode.getPlugin(senderAddress); // Just in case the Node supports multiple links.
//    assertThat(senderPlugin.getPluginSettings().getLocalNodeAddress(), is(senderAddress));
//    assertThat(senderPlugin.isConnected(), is(true));
//
//    final Optional<InterledgerResponsePacket> responsePacket =
//      senderPlugin.ping(destinationAddress).get(TIMEOUT, TimeUnit.SECONDS);
//
//    new InterledgerResponsePacketHandler() {
//      @Override
//      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
//        assertThat(interledgerFulfillPacket.getFulfillment().validateCondition(PING_PROTOCOL_CONDITION), is(true));
//      }
//
//      @Override
//      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
//        fail("Ping request rejected, but should have fulfilled!");
//      }
//
//      @Override
//      protected void handleExpiredPacket() {
//        fail("Ping request expired, but should have fulfilled!");
//      }
//    }.handle(responsePacket);
//  }
}
