package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.CircuitBreakingLink;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology;
import org.interledger.ilpv4.connector.it.topology.LinkNode;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.connector.link.PingableLink.PING_PROTOCOL_CONDITION;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.ALICE;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.ALICE_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.BOB;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.BOB_ADDRESS;
import static org.junit.Assert.assertThat;

/**
 * Tests to verify that a single connector can become a child connector of a parent connector running IL-DCP. In this
 * IT, the Bob connector is a child of Alice.
 */
public class TwoConnectorIldcpIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorIldcpIT.class);
  private static Topology topology = TwoConnectorParentChildBlastTopology.init();

  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  @BeforeClass
  public static void setupClass() {
    System.setProperty("spring.profiles.active", ConnectorProfile.CONNECTOR_MODE + "," + ConnectorProfile.DEV);
    System.setProperty(ConnectorProperties.BLAST_ENABLED, "true");

    LOGGER.info("Starting test topology `{}`...", "TwoConnectorPeerBlastTopology");
    topology.start();
    LOGGER.info("Test topology `{}` started!", "TwoConnectorPeerBlastTopology");
  }

  @AfterClass
  public static void shutdownClass() {
    LOGGER.info("Stopping test topology `{}`...", "TwoConnectorPeerBlastTopology");
    topology.stop();
    LOGGER.info("Test topology `{}` stopped!", "TwoConnectorPeerBlastTopology");
  }

  @Before
  public void setup() {
    aliceConnector = this.getILPv4NodeFromGraph(ALICE_ADDRESS);
    bobConnector = this.getILPv4NodeFromGraph(BOB_ADDRESS);

    // Reset all accounts on each connector...
    bobConnector.getBalanceTracker().resetBalance(AccountId.of(ALICE));
    aliceConnector.getBalanceTracker().resetBalance(AccountId.of(BOB));
  }

  @Test
  public void testAliceNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(ALICE_ADDRESS);
    assertThat(
      connector.getConnectorSettings().getOperatorAddress().get(),
      is(ALICE_ADDRESS)
    );

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);
    assertThat(blastLink.getLinkSettings().getOutgoingAccountId(), is(ALICE));
    assertThat(blastLink.getLinkSettings().getIncomingAccountId(), is(BOB));
  }

  @Test
  public void testBobNodeSettings() throws InterruptedException {
    // Give time for IL-DCP to work...
    Thread.sleep(2000);

    final ILPv4Connector connector = getILPv4NodeFromGraph(BOB_ADDRESS);
    assertThat(connector.getConnectorSettings().getOperatorAddress().isPresent(), is(true));
    assertThat(
      connector.getConnectorSettings().getOperatorAddress().get(),
      is(BOB_ADDRESS)
    );

    final BlastLink blastLink = getBlastLinkFromGraph(BOB_ADDRESS);
    assertThat(blastLink.getLinkSettings().getOutgoingAccountId(), is(BOB));
    assertThat(blastLink.getLinkSettings().getIncomingAccountId(), is(ALICE));
  }

  /**
   * Alice and Connie should have an account with each other, so this ping should succeed.
   */
  @Test
  public void testAlicePingsBob() throws InterruptedException {
    this.testPing(ALICE_ADDRESS, BOB_ADDRESS);

    // ALICE
    assertAccountBalance(aliceConnector, AccountId.of(BOB), BigInteger.ZERO);

    // BOB
    assertAccountBalance(bobConnector, AccountId.of(ALICE), BigInteger.valueOf(1L));
  }

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
    return ((ConnectorServer) topology.getNode(interledgerAddress.getValue()).getContentObject()).getContext()
      .getBean(ILPv4Connector.class);
  }

  /**
   * Helper method to obtain an instance of {@link LinkNode} from the topology, based upon its Interledger Address.
   *
   * @param interledgerAddress The unique key of the node to return.
   *
   * @return
   */
  private BlastLink getBlastLinkFromGraph(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    final CircuitBreakingLink circuitBreakingLink =
      (CircuitBreakingLink) getILPv4NodeFromGraph(interledgerAddress).getAccountManager().getAllAccounts()
        .filter(account -> account.getAccountSettings().getLinkType().equals(BlastLink.LINK_TYPE))
        .findFirst()
        .map(Account::getLink)
        .get();
    return (BlastLink) circuitBreakingLink.getLinkDelegate();
  }

  /**
   * Helper method to testing ping functionality.
   *
   * @param senderNodeAddress  The {@link InterledgerAddress} for the node initiating the ILP ping.
   * @param destinationAddress The {@link InterledgerAddress} to ping.
   */
  private void testPing(final InterledgerAddress senderNodeAddress, final InterledgerAddress destinationAddress)
    throws InterruptedException {

    Objects.requireNonNull(senderNodeAddress);
    Objects.requireNonNull(destinationAddress);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);
    final InterledgerResponsePacket responsePacket = blastLink.ping(destinationAddress, BigInteger.ONE);

    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment().validateCondition(PING_PROTOCOL_CONDITION), is(true));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail(String.format("Ping request rejected, but should have fulfilled: %s", interledgerRejectPacket));
        latch.countDown();
      }

    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  private void assertAccountBalance(
    final ILPv4Connector connector,
    final AccountId accountId,
    final BigInteger expectedAmount
  ) {
    assertThat(
      String.format("Incorrect balance for `%s@%s`!", accountId, connector.getNodeIlpAddress().get().getValue()),
      connector.getBalanceTracker().getBalance(accountId).getAmount().get(), is(expectedAmount.intValue())
    );
  }
}
