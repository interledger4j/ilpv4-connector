package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology;
import org.interledger.ilpv4.connector.it.topology.LinkNode;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.connector.link.PingableLink.PING_PROTOCOL_CONDITION;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.ALICE;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.ALICE_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.BOB;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.BOB_ADDRESS;
import static org.junit.Assert.assertThat;

/**
 * Tests to verify that a single connector can route data and money to/from a single child peer. In this test, value is
 * transferred both from Alice->Bob, and then in the opposite direction. Thus, both Alice and Bob sometimes play the
 * role of sender and sometimes play the role of receiver.
 */
// TODO: Once the PING protocol is specified via RFC, extract the PING tests into an abstract super-class. Every IT
//  should excercise PING functionality as a baseline, and thus far both BTP and BLAST duplicate the same PING tests.
public class TwoConnectorBlastPingIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorBlastPingIT.class);
  private static Topology topology = TwoConnectorPeerBlastTopology.init();

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
      is(TwoConnectorPeerBlastTopology.ALICE_ADDRESS)
    );

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);
    assertThat(blastLink.getLinkSettings().getOutgoingAccountId(), is(ALICE));
    assertThat(blastLink.getLinkSettings().getIncomingAccountId(), is(BOB));
  }

  @Test
  public void testBobNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(BOB_ADDRESS);
    assertThat(
      connector.getConnectorSettings().getOperatorAddress().get(),
      is(TwoConnectorPeerBlastTopology.BOB_ADDRESS)
    );

    final BlastLink blastLink = getBlastLinkFromGraph(BOB_ADDRESS);
    assertThat(blastLink.getLinkSettings().getOutgoingAccountId(), is(BOB));
    assertThat(blastLink.getLinkSettings().getIncomingAccountId(), is(ALICE));
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

    final InterledgerResponsePacket responsePacket = blastLink.ping(ALICE_ADDRESS, BigInteger.ONE);

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
        assertThat(interledgerRejectPacket.getTriggeredBy().isPresent(), is(true));
        assertThat(interledgerRejectPacket.getTriggeredBy().get(), is(BOB_ADDRESS));
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

    // ALICE
    assertAccountBalance(aliceConnector, AccountId.of(BOB), BigInteger.ZERO);

    // BOB
    assertAccountBalance(bobConnector, AccountId.of(ALICE), BigInteger.valueOf(1L));
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

    final InterledgerResponsePacket responsePacket = blastLink.ping(randomDestination, BigInteger.ONE);

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
        assertThat(interledgerRejectPacket.getTriggeredBy().isPresent(), is(true));
        assertThat(interledgerRejectPacket.getTriggeredBy().get(), is(BOB_ADDRESS));
        latch.countDown();
      }
    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /////////////////
  // Expiry Filter
  /////////////////

  /**
   * Random address should reject since it's not in the Connector's routing table.
   */
  @Test
  public void testAlicePingsBobWithExpiredPacket() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(PING_PROTOCOL_CONDITION)
      .expiresAt(Instant.now().minusSeconds(500))
      .amount(BigInteger.valueOf(1L)) // Ping with the smallest unit...
      .destination(BOB_ADDRESS)
      .build();

    final InterledgerResponsePacket responsePacket = blastLink.sendPacket(pingPacket);
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Ping request fulfilled, but should have rejected: %s", interledgerFulfillPacket));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT));
        latch.countDown();
      }

    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /////////////////
  // Max Packet Amount Filter
  /////////////////

  /**
   * Random address should reject since it's not in the Connector's routing table.
   */
  @Test
  public void testAlicePingsBobWithAmountTooHigh() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(PING_PROTOCOL_CONDITION)
      .expiresAt(Instant.now().plusSeconds(30))
      .amount(BigInteger.valueOf(100000000000L)) // Ping with a unit that's too large...
      .destination(BOB_ADDRESS)
      .build();

    final InterledgerResponsePacket responsePacket = blastLink.sendPacket(pingPacket);
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Ping request fulfilled, but should have rejected: %s", interledgerFulfillPacket));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE));
        latch.countDown();
      }

    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /////////////////
  // Allowed Destination Filter
  /////////////////

  /**
   * Disallowed addresses should reject.
   */
  @Test
  public void testAlicePingsDisallowedAddress() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(PING_PROTOCOL_CONDITION)
      .expiresAt(Instant.now().plusSeconds(30))
      .amount(BigInteger.valueOf(1L)) // Ping with a unit that's too large...
      .destination(InterledgerAddress.of("self.foo"))
      .build();

    final InterledgerResponsePacket responsePacket = blastLink.sendPacket(pingPacket);
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Ping request fulfilled, but should have rejected: %s", interledgerFulfillPacket));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
        latch.countDown();
      }

    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /////////////////
  // Invalid Condition
  /////////////////

  /**
   * Random address should reject since it's not in the Connector's routing table.
   */
  @Test
  public void testAlicePingsBobWithInvalidCondition() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .expiresAt(Instant.now().plusSeconds(30))
      .amount(BigInteger.valueOf(1L)) // Ping with the smallest unit...
      .destination(BOB_ADDRESS)
      .build();

    final InterledgerResponsePacket responsePacket = blastLink.sendPacket(pingPacket);
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Ping request fulfilled, but should have rejected: %s", interledgerFulfillPacket));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
        latch.countDown();
      }

    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  ///////////////////////
  // Average Fulfill Time
  ///////////////////////

  /**
   * Attempts to ping 100k packets (i.e., 100k fulfills) and measures the time it takes, failing if the time takes
   * longer than some upper bound.
   *
   * Note: this upper-bound may be adjusted as the Connector implementation has perf improvements or degredations.
   */
  @Test
  public void testPingPerf() throws InterruptedException {
    final BlastLink aliceBlastLink = getBlastLinkFromGraph(ALICE_ADDRESS);

    final int numReps = 2000;

    final int numThreads = 10;
    final CountDownLatch latch = new CountDownLatch(numReps);

    // Tracks total time spent in the Runnable so we can find an average.
    AtomicInteger totalMillis = new AtomicInteger();

    final Runnable runnable = () -> {
      final long start = System.currentTimeMillis();
      final InterledgerResponsePacket responsePacket = aliceBlastLink.ping(BOB_ADDRESS, BigInteger.ONE);

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
      final long end = System.currentTimeMillis();
      totalMillis.getAndAdd((int) (end - start));
    };

    final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    final long start = System.currentTimeMillis();
    for (int i = 0; i < numReps; i++) {
      executor.submit(runnable);
    }
    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    double totalTime = (end - start);

    double averageProcssingTime = ((double) (totalMillis.get()) / numReps);
    double averageMsPerPing = totalTime / numReps;

    LOGGER.info("[Pings Perf Test] Latch Count: {}", latch.getCount());
    LOGGER.info("[Pings Perf Test] {} pings took {} ms", numReps, totalTime);
    LOGGER.info("[Pings Perf Test] Each Ping spent {} ms processing, on Average", averageProcssingTime);
    LOGGER.info("[Pings Perf Test] Average ms/ping: {} ms", averageMsPerPing);

    assertThat(latch.getCount(), is(0L));
    assertThat(averageProcssingTime < 20, is(true));
    assertThat(averageMsPerPing < 2, is(true));

    assertAccountBalance(aliceConnector, AccountId.of(BOB), BigInteger.ZERO);
    assertAccountBalance(bobConnector, AccountId.of(ALICE), BigInteger.valueOf(numReps));
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
