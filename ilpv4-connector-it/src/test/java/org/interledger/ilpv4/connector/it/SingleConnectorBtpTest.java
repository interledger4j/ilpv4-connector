package org.interledger.ilpv4.connector.it;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.ILPv4Node;
import org.interledger.ilpv4.ILPv4Receiver;
import org.interledger.ilpv4.ILPv4Sender;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.PluginNode;
import org.interledger.ilpv4.connector.it.topologies.SingleConnectorBtpTopology;
import org.interledger.plugin.lpiv2.Plugin;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.sappenin.interledger.ilpv4.connector.plugins.connectivity.PingProtocolPlugin.PING_PROTOCOL_CONDITION;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.ilpv4.connector.it.topologies.SingleConnectorBtpTopology.ALICE;
import static org.interledger.ilpv4.connector.it.topologies.SingleConnectorBtpTopology.BOB;
import static org.interledger.ilpv4.connector.it.topologies.SingleConnectorBtpTopology.CONNIE;
import static org.junit.Assert.assertThat;

/**
 * Tests to verify that a single connector can route data and money between two child peers. In this test, value is
 * transferred both from Alice->Connector->Bob, and in the opposite direction. Thus, both Alice and Bob sometimes play
 * the role of sender (via {@link ILPv4Sender}), and sometimes play the role of receiver (via {@link ILPv4Receiver}.
 */
public class SingleConnectorBtpTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorBtpTest.class);
  private static Graph graph = SingleConnectorBtpTopology.init();
  private final int TIMEOUT = 2;

  @BeforeClass
  public static void setup() {
    LOGGER.info("Starting test graph `{}`...", "SingleConnectorBtpTopology");
    graph.start();
    LOGGER.info("Test graph `{}` started!", "SingleConnectorBtpTopology");
  }

  @AfterClass
  public static void shutdown() {
    LOGGER.info("Stopping test graph `{}`...", "SingleConnectorBtpTopology");
    graph.stop();
    LOGGER.info("Test graph `{}` stopped!", "SingleConnectorBtpTopology");
  }

  @Test
  public void testAliceNodeSettings() {
    final Plugin client = getPluginNodeFromGraph(ALICE).getPlugin(ALICE);
    assertThat(client.getPluginSettings().getLocalNodeAddress(), is(ALICE));
    assertThat(client.isConnected(), is(true));
  }

  @Test
  public void testConnieNodeSettings() {
    final Plugin alicePluginInConnie = getPluginNodeFromGraph(CONNIE).getPlugin(ALICE);
    assertThat(alicePluginInConnie.getPluginSettings().getLocalNodeAddress(), is(CONNIE));
    assertThat(alicePluginInConnie.getPluginSettings().getPeerAccountAddress(), is(ALICE));

    final Plugin bobPluginInConnie = getPluginNodeFromGraph(CONNIE).getPlugin(BOB);
    assertThat(bobPluginInConnie.getPluginSettings().getLocalNodeAddress(), is(CONNIE));
    assertThat(bobPluginInConnie.getPluginSettings().getPeerAccountAddress(), is(BOB));
  }

  @Test
  public void testBobNodeSettings() {
    final Plugin client = getPluginNodeFromGraph(BOB).getPlugin(BOB);
    assertThat(client.getPluginSettings().getLocalNodeAddress(), is(BOB));
    assertThat(client.isConnected(), is(true));
  }

  @Test
  public void testAlicePingsConnie() throws InterruptedException, ExecutionException, TimeoutException {
    this.testPing(ALICE, CONNIE);
  }

  @Test
  public void testAlicePingsBob() throws InterruptedException, ExecutionException, TimeoutException {
    this.testPing(ALICE, BOB);
  }

  /////////////////
  // Helper Methods
  /////////////////

  /**
   * Helper method to obtain an instance of {@link ILPv4Connector} from the graph, based upon its Interledger Address.
   *
   * @param interledgerAddress
   *
   * @return
   */
  private ILPv4Node getILPv4NodeFromGraph(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    return (ILPv4Node) graph.getNode(interledgerAddress).getContentObject();
  }

  /**
   * Helper method to obtain an instance of {@link PluginNode} from the graph, based upon its Interledger Address.
   *
   * @param interledgerAddress The unique key of the node to return.
   *
   * @return
   */
  private PluginNode<?> getPluginNodeFromGraph(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    return (PluginNode) graph.getNode(interledgerAddress);
  }

  private void testPing(
    final InterledgerAddress senderAddress, final InterledgerAddress destinationAddress
  ) throws InterruptedException, ExecutionException, TimeoutException {

    Objects.requireNonNull(senderAddress);
    Objects.requireNonNull(destinationAddress);

    final PluginNode<?> senderNode = getPluginNodeFromGraph(senderAddress);
    final Plugin senderPlugin = senderNode.getPlugin(senderAddress); // Just in case the Node supports multiple plugins.
    assertThat(senderPlugin.getPluginSettings().getLocalNodeAddress(), is(senderAddress));
    assertThat(senderPlugin.isConnected(), is(true));

    final Optional<InterledgerResponsePacket> responsePacket =
      senderPlugin.ping(destinationAddress).get(TIMEOUT, TimeUnit.SECONDS);

    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment().validateCondition(PING_PROTOCOL_CONDITION), is(true));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail("Ping request rejected, but should have fulfilled!");
      }

      @Override
      protected void handleExpiredPacket() {
        fail("Ping request expired, but should have fulfilled!");
      }
    }.handle(responsePacket);
  }
}
