package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorBlastTopology;
import org.interledger.ilpv4.connector.it.topology.LinkNode;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.sappenin.interledger.ilpv4.connector.links.ping.PingProtocolLink.PING_PROTOCOL_CONDITION;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorBlastTopology.ALICE_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorBlastTopology.BOB_ADDRESS;
import static org.junit.Assert.assertThat;

/**
 * Tests to verify that a single connector can route data and money to/from a single child peer. In this test, value is
 * transferred both from Alice->Chloe, and then in the opposite direction. Thus, both Alice and Chloe sometimes play the
 * role of sender and sometimes play the role of receiver.
 */
// TODO: Once the PING protocol is specified via RFC, extract the PING tests into an abstract super-class. Every IT
//  should excercise PING functionality as a baseline, and thus far both BTP and BLAST duplicate the same PING tests.
public class TwoConnectorBlastIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorBlastIT.class);
  private static Topology topology = TwoConnectorBlastTopology.init();

  @BeforeClass
  public static void setup() {
    System.setProperty("spring.profiles.active", ConnectorProfile.CONNECTOR_MODE + "," + ConnectorProfile.DEV);
    System.setProperty(ConnectorProperties.BLAST_ENABLED, "true");

    LOGGER.info("Starting test topology `{}`...", "TwoConnectorBlastTopology");
    topology.start();
    LOGGER.info("Test topology `{}` started!", "TwoConnectorBlastTopology");
  }

  @AfterClass
  public static void shutdown() {
    LOGGER.info("Stopping test topology `{}`...", "TwoConnectorBlastTopology");
    topology.stop();
    LOGGER.info("Test topology `{}` stopped!", "TwoConnectorBlastTopology");
  }

  @Test
  public void testAliceNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(ALICE_ADDRESS);
    assertThat(connector.getConnectorSettings().getOperatorAddress(), is(TwoConnectorBlastTopology.ALICE_ADDRESS));

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);
    assertThat(blastLink.getLinkSettings().getOperatorAddress(), is(ALICE_ADDRESS));
  }

  @Test
  public void testBobNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(BOB_ADDRESS);
    assertThat(connector.getConnectorSettings().getOperatorAddress(), is(TwoConnectorBlastTopology.BOB_ADDRESS));

    final BlastLink blastLink = getBlastLinkFromGraph(BOB_ADDRESS);
    assertThat(blastLink.getLinkSettings().getOperatorAddress(), is(TwoConnectorBlastTopology.BOB_ADDRESS));
  }

  /**
   * Pinging yourself is not currently allowed because an account is generally not routed to itself, so this should
   * reject.
   */
  @Test
  public void testAlicePingsAlice() throws InterruptedException {
    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final Optional<InterledgerResponsePacket> responsePacket =
      blastLink.ping(ALICE_ADDRESS);

    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Ping request fulfilled, but should have rejected: %s)", interledgerFulfillPacket));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
        assertThat(interledgerRejectPacket.getMessage(), is("Destination address is unreachable"));
        assertThat(interledgerRejectPacket.getTriggeredBy(), is(BOB_ADDRESS));
        latch.countDown();
      }

      @Override
      protected void handleExpiredPacket() {
        fail("Ping request expired, but should have fulfilled!");
        latch.countDown();
      }
    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /**
   * Alice and Connie should have an account with each other, so this ping should succeed.
   */
  @Test
  public void testAlicePingsBob() throws InterruptedException {
    this.testPing(ALICE_ADDRESS, BOB_ADDRESS);
  }

  /**
   * Random address should reject since it's not in the Connector's routing table.
   */
  @Test
  public void testAlicePingsRandom() throws InterruptedException {
    final InterledgerAddress randomDestination =
      InterledgerAddress.of(InterledgerAddressPrefix.TEST3.with(UUID.randomUUID().toString()).getValue());

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final Optional<InterledgerResponsePacket> responsePacket = blastLink.ping(randomDestination);

    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Ping request fulfilled, but should have rejected: %s)", interledgerFulfillPacket));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
        assertThat(interledgerRejectPacket.getMessage(), is("Destination address is unreachable"));
        assertThat(interledgerRejectPacket.getTriggeredBy(), is(BOB_ADDRESS));
        latch.countDown();
      }

      @Override
      protected void handleExpiredPacket() {
        fail("Ping request expired, but should have fulfilled!");
        latch.countDown();
      }
    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
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
    return ((ConnectorServer) topology.getNode(interledgerAddress).getContentObject()).getContext()
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
    return (BlastLink) getILPv4NodeFromGraph(interledgerAddress).getAccountManager().getAllAccounts()
      .filter(account -> account.getAccountSettings().getLinkType().equals(BlastLink.LINK_TYPE))
      .findFirst()
      .map(Account::getLink)
      .get();
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
    final Optional<InterledgerResponsePacket> responsePacket =
      blastLink.ping(destinationAddress);

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

      @Override
      protected void handleExpiredPacket() {
        fail("Ping request expired, but should have fulfilled!");
        latch.countDown();
      }
    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }
}
