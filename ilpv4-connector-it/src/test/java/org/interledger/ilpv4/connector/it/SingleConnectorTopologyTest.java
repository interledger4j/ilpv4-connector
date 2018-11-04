package org.interledger.ilpv4.connector.it;

import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.client.BtpWsClient;
import com.sappenin.ilpv4.client.IlpClient;
import org.interledger.core.*;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.ServerNode;
import org.interledger.ilpv4.connector.it.graph.nodes.IlpSenderNode;
import org.interledger.ilpv4.connector.it.topologies.SingleConnectorTopology;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.btp2.BtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.events.PluginEventHandler;
import org.interledger.plugin.lpiv2.exceptions.DataHandlerAlreadyRegisteredException;
import org.interledger.plugin.lpiv2.exceptions.MoneyHandlerAlreadyRegisteredException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static com.sappenin.ilpv4.packetswitch.preemptors.EchoController.ECHO_DATA_PREFIX;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.ilpv4.connector.it.BtpArchitectures.ALICE;
import static org.interledger.ilpv4.connector.it.BtpArchitectures.CONNIE;
import static org.interledger.ilpv4.connector.it.graph.nodes.ConnectorNode.CONNECTOR_BEAN;
import static org.interledger.ilpv4.connector.it.topologies.SingleConnectorTopology.BOB;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests to verify that a single connector can route data and money for a single sender and a single receiver.
 */
public class SingleConnectorTopologyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorTopologyTest.class);
  private static Graph graph = SingleConnectorTopology.init();

  @BeforeClass
  public static void setup() {
    LOGGER.info("Starting test graph `{}`...", "SingleConnectorTopology");
    graph.start();
    LOGGER.info("Test graph `{}` started!", "SingleConnectorTopology");
  }

  @AfterClass
  public static void shutdown() {
    LOGGER.info("Stopping test graph `{}`...", "SingleConnectorTopology");
    graph.stop();
    LOGGER.info("Test graph `{}` stopped!", "SingleConnectorTopology");
  }

  @Test
  public void testAliceNodeSettings() {
    final IlpClient aliceClient = getIlpClientFromGraph(ALICE);
    assertThat(aliceClient.getPluginSettings().getLocalNodeAddress(), is(ALICE));
  }

  @Test
  public void testConnieNodeSettings() {
    final IlpConnector connieConnector = getIlpConnectorFromGraph(CONNIE);
    assertThat(connieConnector.getConnectorSettings().getIlpAddress(), is(CONNIE));
  }

  @Test
  public void testBobNodeSettings() {
    final IlpClient bobClient = getIlpClientFromGraph(BOB);
    assertThat(bobClient.getPluginSettings().getLocalNodeAddress(), is(BOB));
  }



  @Test
  public void testAlicePingsParent() throws ExecutionException, InterruptedException, TimeoutException {
    final IlpConnector connieConnector = getIlpConnectorFromGraph(CONNIE);
    assertThat(connieConnector.getConnectorSettings().getIlpAddress(), is(CONNIE));

    final IlpClient<PluginSettings> aliceClient = getIlpClientFromGraph(ALICE);
    assertThat(aliceClient.getPluginSettings().getLocalNodeAddress(), is(ALICE));
    assertThat(aliceClient.getPluginDelegate().isConnected(), is(true));

    final UUID pingId = UUID.randomUUID();
    final String pingData = String.format("%s\n%s\n%s", ECHO_DATA_PREFIX, pingId, ALICE.getValue());

    final InterledgerPreparePacket echoPacket = InterledgerPreparePacket.builder()
      .destination(CONNIE)
      .expiresAt(Instant.now().plusSeconds(500))
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .data(pingData.getBytes(StandardCharsets.US_ASCII))
      .build();

    final CountDownLatch countDownLatch = new CountDownLatch(1);

    final InterledgerFulfillPacket interledgerFulfillPacket =
      aliceClient.sendData(echoPacket).whenComplete(((fulfillPacket, throwable) -> countDownLatch.countDown())).get();

    countDownLatch.await(5, TimeUnit.MINUTES);
    assertThat(
      new String(interledgerFulfillPacket.getData()),
      is(String.format("%s\n%s", "PONG", pingId.toString()))
    );
  }

  @Test
  public void testAlicePingsPeer() {
    fail();
    //      final IlpConnector aliceConnector = getIlpConnectorFromGraph(ALICE);
    //      assertThat(aliceConnector.getConnectorSettings().getIlpAddress(), is(ALICE));
    //
    //      final UUID pingId = UUID.randomUUID();
    //      final String pingData = String.format("%s\n%s\n%s", "ECHOECHOECHOECHO", pingId, ALICE.getValue());
    //
    //      final InterledgerPreparePacket echoPacket = InterledgerPreparePacket.builder()
    //        .destination(BOB)
    //        .expiresAt(Instant.now().plusSeconds(500))
    //        .executionCondition(InterledgerCondition.of(new byte[32]))
    //        .data(pingData.getBytes(StandardCharsets.US_ASCII))
    //        .build();
    //
    //      final InterledgerFulfillPacket interledgerFulfillPacket =
    //        aliceConnector.getIlpPluginDataHandler().handleIncomingData(ALICE, echoPacket).get();
    //      assertThat(
    //        new String(interledgerFulfillPacket.getData()),
    //        is(String.format("%s\n%s", "PONG", pingId.toString()))
    //      );
  }

  /////////////////
  // Helper Methods
  /////////////////

  /**
   * Helper method to obtain an instance of {@link IlpConnector} from the graph, based upon its Interledger Address.
   *
   * @param interledgerAddress
   *
   * @return
   */
  private IlpConnector getIlpConnectorFromGraph(final InterledgerAddress interledgerAddress) {
    return (IlpConnector) ((ServerNode) graph.getNode(interledgerAddress))
      .getServer().getContext().getBean(CONNECTOR_BEAN);
  }

  /**
   * Helper method to obtain an instance of {@link IlpConnector} from the graph, based upon its Interledger Address.
   *
   * @param interledgerAddress
   *
   * @return
   */
  private IlpClient getIlpClientFromGraph(final InterledgerAddress interledgerAddress) {
    return ((IlpSenderNode) graph.getNode(interledgerAddress)).getIlpClient();
  }

}
