package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
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
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.connector.link.PingableLink.PING_PROTOCOL_CONDITION;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.BOB_AT_ALICE_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.PAUL_ACCOUNT;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.PAUL_AT_ALICE_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.ALICE;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.BOB;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.BOB_CONNECTOR_ADDRESS;
import static org.junit.Assert.assertThat;

/**
 * Tests to verify that a single connector can route data and money to/from a single child peer. In this test, value is
 * transferred both from Alice->Bob, and then in the opposite direction. Thus, both Alice and Bob sometimes play the
 * role of sender and sometimes play the role of receiver.
 */
// TODO: Once the PING protocol is specified via RFC, extract the PING tests into an abstract super-class. Every IT
//  should exercise PING functionality as a baseline, but both BTP and BLAST duplicate the same PING tests.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TwoConnectorBlastPingTestIT extends AbstractBlastIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorBlastPingTestIT.class);
  private static Topology topology = TwoConnectorPeerBlastTopology.init();

  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  @BeforeClass
  public static void startTopology() {
    LOGGER.info("Starting test topology `{}`...", "TwoConnectorPeerBlastTopology");
    topology.start();
    LOGGER.info("Test topology `{}` started!", "TwoConnectorPeerBlastTopology");
  }

  @AfterClass
  public static void stopTopology() {
    LOGGER.info("Stopping test topology `{}`...", "TwoConnectorPeerBlastTopology");
    topology.stop();
    LOGGER.info("Test topology `{}` stopped!", "TwoConnectorPeerBlastTopology");
  }

  @Before
  public void setup() {
    aliceConnector = this.getILPv4NodeFromGraph(ALICE_CONNECTOR_ADDRESS);
    // Note Bob's Connector's address is purposefully a child of Alice due to IL-DCP
    bobConnector = this.getILPv4NodeFromGraph(BOB_AT_ALICE_ADDRESS);
    this.resetBalanceTracking();
  }

  @Test
  public void testAliceNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(ALICE_CONNECTOR_ADDRESS);
    assertThat(connector.getConnectorSettings().getOperatorAddress().get(), is(ALICE_CONNECTOR_ADDRESS));

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, BOB_ACCOUNT);
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().tokenSubject(), is(ALICE));
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(blastLink.getLinkSettings().incomingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
  }

  @Test
  public void testBobNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(BOB_CONNECTOR_ADDRESS);
    assertThat(connector.getConnectorSettings().getOperatorAddress().get(), is(BOB_CONNECTOR_ADDRESS));

    final BlastLink blastLink = getBlastLinkFromGraph(BOB_CONNECTOR_ADDRESS, ALICE_ACCOUNT);
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().tokenSubject(), is(BOB));
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(blastLink.getLinkSettings().incomingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
  }

  /**
   * Pinging a non-connector account (i.e., yourself) is not allowed because ping is only meant to be used at a
   * Connector level. Thus, this should ping request should reject.
   */
  @Test
  public void testPaulPingsPaulChildAccount() throws InterruptedException {
    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    // Note Bob's Connector's address is purposefully a child of Alice due to IL-DCP
    final InterledgerResponsePacket responsePacket = blastLink.ping(PAUL_AT_ALICE_ADDRESS, ONE);

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
        assertThat(interledgerRejectPacket.getTriggeredBy().get(), is(ALICE_CONNECTOR_ADDRESS));
        latch.countDown();
      }
    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /**
   * Pinging a local connector account (i.e., Paul to his own Connector) is allowed.
   */
  @Test
  public void testPaulPingsAliceConnector() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, ALICE_CONNECTOR_ADDRESS, ALICE_CONNECTOR_ADDRESS, ONE);

    // test.alice.paul: Should be -1 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ONE.negate());
    // test.alice.bob: Should be 0 because this account is never engaged.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    // test.alice.__ping_account__: Should be +1 because this account fulfills and is actually receiving money.
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ONE);

    // test.bob.alice: Should be 0 because this account is never engaged.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    // test.bob.__ping_account__: Should be 0 because this account is never engaged.
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, ZERO);
  }

  /**
   * In this setup, the peering account that Alice uses to talk to Bob is used to ping Alice. Here, the outbound link
   * will place a ping packet on the link, sending it directly to Bob, who will try to route it back to Alice. However,
   * the next-hop routing logic will detect a routing loop, and reject the packet.
   */
  @Test
  public void testAlicePingsAliceUsingBobAccount() throws InterruptedException {
    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, BOB_ACCOUNT);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final InterledgerResponsePacket responsePacket = blastLink.ping(ALICE_CONNECTOR_ADDRESS, ONE);

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
        assertThat(interledgerRejectPacket.getTriggeredBy().get(), is(BOB_CONNECTOR_ADDRESS));
        latch.countDown();
      }
    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /**
   * Paul (a child account of Alice) pings Bob via Alice, which should succeed using 0 units.
   */
  @Test
  public void testPaulPingsBobConnectorWith0Units() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, ALICE_CONNECTOR_ADDRESS, BOB_CONNECTOR_ADDRESS, ZERO);

    // test.alice.paul: Should be 0 because the packet units are 0
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ZERO);
    // test.alice.bob: Should be 0 because the packet units are 0
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    // test.alice.__ping_account__: Should be 0 because the packet units are 0
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    // test.bob.alice: Should be 0 because the packet units are 0
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    // test.bob.__ping_account__: Untouched
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, ZERO);
  }

  /**
   * Paul (a child account of Alice) pings Bob via Alice, which should succeed using 1 units.
   */
  @Test
  public void testPaulPingsBobConnectorWith1Units() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, ALICE_CONNECTOR_ADDRESS, BOB_CONNECTOR_ADDRESS, ONE);

    // test.alice.paul: Should be -1 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ONE.negate());
    // test.alice.bob: Should be 0 because this account will receive one from Paul, but then pay the Bob Connector.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ONE);
    // test.alice.__ping_account__: Should be 0 because it is no engaged in this flow.
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    // test.bob.alice: Should be 0 because it gets 1 from Alice Connector, but pays one to the ping account on Bob.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ONE.negate());
    // test.bob.__ping_account__: Should be +1 because it's receiving the ping funds.  Because this account is owned
    // by the Connector, it's OK to extend the 1 unit of credit above to the incoming account.
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, ONE);
  }

  /**
   * Paul (a child account of Alice) pings Bob via Alice, which should succeed using 10 units.
   */
  @Test
  public void testPaulPingsBobWith10Units() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, ALICE_CONNECTOR_ADDRESS, BOB_CONNECTOR_ADDRESS, BigInteger.TEN);

    // test.alice.paul: Should be -10 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, TEN.negate());
    // test.alice.bob: Should be 10 because this account will receive ten from Paul on this Connector.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, TEN);
    // test.alice.__ping_account__: Should be 0 because it is no engaged in this flow.
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    // test.bob.alice: Should be -10 because it pays from Alice Connector, but pays one to the ping account on Bob.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, TEN.negate());
    // test.bob.__ping_account__: Should be +10 because it's receiving the ping funds.  Because this account is owned
    // by the Connector, it's OK to extend the 1 unit of credit above to the incoming account.
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, TEN);
  }

  /**
   * Random address should reject since it's not in the Bob's routing table.
   */
  @Test
  public void testPaulPingsRandom() throws InterruptedException {
    final InterledgerAddress randomDestination =
      InterledgerAddress.of(InterledgerAddressPrefix.TEST3.with(UUID.randomUUID().toString()).getValue());

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final InterledgerResponsePacket responsePacket = blastLink.ping(randomDestination, ONE);

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
        assertThat(interledgerRejectPacket.getTriggeredBy().get(), is(ALICE_CONNECTOR_ADDRESS));
        latch.countDown();
      }
    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }


  /**
   * Random address should reject since it's not in the Bob's routing table.
   */
  @Test
  public void testPaulPingsRandomAtBob() throws InterruptedException {
    final InterledgerAddress randomDestination = BOB_CONNECTOR_ADDRESS.with(UUID.randomUUID().toString());

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final InterledgerResponsePacket responsePacket = blastLink.ping(randomDestination, ONE);

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
        assertThat(interledgerRejectPacket.getTriggeredBy().get(), is(BOB_CONNECTOR_ADDRESS));
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

  @Test
  public void testPaulPingsBobWithExpiredPacket() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(PING_PROTOCOL_CONDITION)
      .expiresAt(Instant.now().minusSeconds(500))
      .amount(BigInteger.valueOf(1L)) // Ping with the smallest unit...
      .destination(BOB_CONNECTOR_ADDRESS)
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
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT));
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

  @Test
  public void testPaulPingsBobWithAmountTooHigh() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(PING_PROTOCOL_CONDITION)
      .expiresAt(Instant.now().plusSeconds(30))
      .amount(BigInteger.valueOf(100000000000L)) // Ping with a unit that's too large...
      .destination(BOB_CONNECTOR_ADDRESS)
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

  @Test
  public void testPaulPingsDisallowedAddress() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

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

  @Test
  public void testPaulPingsBobWithInvalidCondition() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .expiresAt(Instant.now().plusSeconds(30))
      .amount(BigInteger.valueOf(1L)) // Ping with the smallest unit...
      .destination(BOB_CONNECTOR_ADDRESS)
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
   * Attempts to ping `numReps` packets and measures the time it takes, failing if the time takes longer than some upper
   * bound.
   *
   * Note: this upper-bound may be adjusted as the Connector implementation has perf improvements or degradations.
   */
  @Test
  public void zTestPingPerf() throws InterruptedException {
    final BlastLink paulToAliceLink = getBlastLinkFromGraph(ALICE_CONNECTOR_ADDRESS, PAUL_ACCOUNT);

    final int TIMEOUT = 30;
    final int numReps = 2000;

    final int numThreads = 5;
    final CountDownLatch latch = new CountDownLatch(numReps);

    // Tracks total time spent in the Runnable so we can find an average.
    AtomicInteger totalMillis = new AtomicInteger();

    final Runnable runnable = () -> {
      final long start = System.currentTimeMillis();
      final InterledgerResponsePacket responsePacket = paulToAliceLink.ping(BOB_CONNECTOR_ADDRESS, ONE);

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
    latch.await(TIMEOUT, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();

    executor.shutdown();
    executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS);

    double totalTime = (end - start);

    double averageProcessingTime = ((double) (totalMillis.get()) / numReps);
    double averageMsPerPing = totalTime / numReps;

    LOGGER.info("[Pings Perf Test] Latch Count: {}", latch.getCount());
    LOGGER.info("[Pings Perf Test] {} pings took {} ms", numReps, totalTime);
    LOGGER.info("[Pings Perf Test] Average CPU (time/ping): {} ms", averageProcessingTime);
    LOGGER.info("[Pings Perf Test] Average Ping (ms/ping): {} ms", averageMsPerPing);

    assertThat(latch.getCount(), is(0L));
    assertThat(
      "averageProcessingTime should have been less than 20, but was " + averageProcessingTime,
      averageProcessingTime < 20,
      is(true)
    );
    assertThat(
      "averageMsPerPing should have been less than 4, but was " + averageMsPerPing, averageMsPerPing < 4,
      is(true)
    );

    // test.alice.paul: Should be -1 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, BigInteger.valueOf(numReps * -1));
    // test.alice.bob: Should be 0 because this account will receive one from Paul, but then pay the Bob Connector.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, BigInteger.valueOf(numReps));
    // test.alice.__ping_account__: Should be 0 because it is no engaged in this flow.
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    // test.bob.alice: Should be 0 because it gets `numReps` from Alice Connector, but pays `numReps` to the ping
    // account on Bob.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, BigInteger.valueOf(numReps * -1));
    // test.bob.__ping_account__: Should be `numReps` because it's receiving the ping funds.  Because this account is
    // owned by the Connector, it's OK to extend the 1 unit of credit above to the incoming account.
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, BigInteger.valueOf(numReps));
  }

  // TODO: Cross currency ping?

  /////////////////
  // Helper Methods
  /////////////////

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected Topology getTopology() {
    return topology;
  }
}
