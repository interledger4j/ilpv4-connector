package org.interledger.ilpv4.connector.it;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.ILPv4Node;
import org.interledger.ilpv4.connector.it.topologies.btp.SingleConnectorSingleAccountBtpTopology;
import org.interledger.ilpv4.connector.it.topology.PluginNode;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.interledger.plugin.lpiv2.btp2.spring.BtpClientPlugin;
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
import static org.interledger.ilpv4.connector.it.topologies.btp.SingleConnectorSingleAccountBtpTopology.ALICE;
import static org.interledger.ilpv4.connector.it.topologies.btp.SingleConnectorSingleAccountBtpTopology.CONNIE;
import static org.junit.Assert.assertThat;

/**
 * Tests to verify that a single connector can route data and money to/from a single child peer. In this test, value is
 * transferred both from Alice->Chloe, and then in the opposite direction. Thus, both Alice and Chloe sometimes play the
 * role of sender and sometimes play the role of receiver.
 */
public class SingleConnectorSingleAccountBtpTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorSingleAccountBtpTest.class);
  private static Topology topology = SingleConnectorSingleAccountBtpTopology.init();
  private final int TIMEOUT = 2;

  @BeforeClass
  public static void setup() {
    System.setProperty("spring.profiles.active", ConnectorProfile.CONNECTOR_MODE + "," + ConnectorProfile.DEV);
    System.setProperty(ConnectorProperties.BTP_ENABLED, "true");

    LOGGER.info("Starting test topology `{}`...", "SingleConnectorMultiAccountBtpTopology");
    topology.start();
    LOGGER.info("Test topology `{}` started!", "SingleConnectorMultiAccountBtpTopology");
  }

  @AfterClass
  public static void shutdown() {
    LOGGER.info("Stopping test topology `{}`...", "SingleConnectorMultiAccountBtpTopology");
    topology.stop();
    LOGGER.info("Test topology `{}` stopped!", "SingleConnectorMultiAccountBtpTopology");
  }

  @Test
  public void testAliceNodeSettings() {
    final BtpClientPlugin clientConnection = getClientNodeFromGraph(ALICE).getContentObject();
    assertThat(clientConnection.getPluginSettings().getOperatorAddress(), is(ALICE));
  }

  @Test
  public void testConnieNodeSettings() {
    final ILPv4Node connieNode = getILPv4NodeFromGraph(CONNIE);
    assertThat(connieNode.getAddress(), is(CONNIE));
  }

  @Test
  public void testAlicePingsAlice() throws InterruptedException, ExecutionException, TimeoutException {
    this.testPing(ALICE, ALICE);
  }

  //
  //  @Test
  //  public void testAlicePingsAliceAtConnie() throws InterruptedException, ExecutionException, TimeoutException {
  //    this.testPing(ALICE, ALICE_AT_CONNIE);
  //  }
  //
  //  @Test
  //  public void testAlicePingsConnie() throws InterruptedException, ExecutionException, TimeoutException {
  //    this.testPing(ALICE, CONNIE);
  //  }
  //
  //  @Test
  //  public void testAlicePingsBob() throws InterruptedException, ExecutionException, TimeoutException {
  //    this.testPing(ALICE, BOB_AT_CONNIE);
  //  }

  /////////////////
  // Helper Methods
  /////////////////

  /**
   * Helper method to obtain an instance of {@link ILPv4Connector} from the topology, based upon its Interledger
   * Address.
   *
   * @param interledgerAddress
   *
   * @return
   */
  private ILPv4Connector getILPv4NodeFromGraph(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    return ((ConnectorServer) topology.getNode(interledgerAddress).getContentObject()).getContext()
      .getBean(ILPv4Connector.class);
  }

  /**
   * Helper method to obtain an instance of {@link PluginNode} from the topology, based upon its Interledger Address.
   *
   * @param interledgerAddress The unique key of the node to return.
   *
   * @return
   */
  private PluginNode<BtpClientPlugin> getClientNodeFromGraph(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    return (PluginNode) topology.getNode(interledgerAddress);
  }

  /**
   * Helper method to testing ping functionality.
   *
   * @param senderNodeAddress  The {@link InterledgerAddress} for the node initiating the ILP ping.
   * @param destinationAddress The {@link InterledgerAddress} to ping.
   */
  private void testPing(
    final InterledgerAddress senderNodeAddress,
    final InterledgerAddress destinationAddress
  ) throws InterruptedException, ExecutionException, TimeoutException {

    Objects.requireNonNull(senderNodeAddress);
    Objects.requireNonNull(destinationAddress);

    final BtpClientPlugin btpClient = getClientNodeFromGraph(ALICE).getContentObject();
    final Optional<InterledgerResponsePacket> responsePacket =
      btpClient.ping(destinationAddress).get(TIMEOUT, TimeUnit.SECONDS);

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
