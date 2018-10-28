package org.interledger.ilpv4.connector.it;

import com.sappenin.ilpv4.IlpConnector;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.ServerNode;
import org.interledger.ilpv4.connector.it.topologies.SingleConnectorTopology;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.interledger.ilpv4.connector.it.BtpArchitectures.ALICE;
import static org.interledger.ilpv4.connector.it.BtpArchitectures.CONNIE;
import static org.interledger.ilpv4.connector.it.graph.ConnectorNode.CONNECTOR_BEAN;
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
    final IlpConnector aliceConnector = getIlpConnectorFromGraph(ALICE);
    assertThat(aliceConnector.getConnectorSettings().getIlpAddress(), is(ALICE));
  }

  @Test
  public void testConnieNodeSettings() {
    final IlpConnector connieConnector = getIlpConnectorFromGraph(CONNIE);
    assertThat(connieConnector.getConnectorSettings().getIlpAddress(), is(CONNIE));
  }

  @Test
  public void testBobNodeSettings() {
    final IlpConnector aliceConnector = getIlpConnectorFromGraph(BOB);
    assertThat(aliceConnector.getConnectorSettings().getIlpAddress(), is(ALICE));
  }

  @Test
  public void testAlicePingsParent() {
    final IlpConnector aliceConnector = getIlpConnectorFromGraph(ALICE);
    assertThat(aliceConnector.getConnectorSettings().getIlpAddress(), is(ALICE));

    //
    //davidConnector.
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
    return (IlpConnector) ((ServerNode) graph.getNode(interledgerAddress.getValue()))
      .getServer().getContext().getBean(CONNECTOR_BEAN);
  }

}
