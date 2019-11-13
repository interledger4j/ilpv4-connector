package org.interledger.connector.it.ilpoverhttp;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL_AT_ALICE_ADDRESS;
import static org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology.ALICE;
import static org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology.BOB;
import static org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology.BOB_CONNECTOR_ADDRESS;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
import static org.interledger.link.PingLoopbackLink.PING_PROTOCOL_CONDITION;
import static org.junit.Assert.assertThat;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.AbstractIlpOverHttpIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.markers.IlpOverHttp;
import org.interledger.connector.it.markers.Performance;
import org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.ping.DefaultPingInitiator;
import org.interledger.connector.ping.PingInitiator;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;

import com.google.common.primitives.UnsignedLong;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests to verify that a single connector can route data and money to/from a single child peer. In this test, value is
 * transferred both from Alice->Bob, and then in the opposite direction. Thus, both Alice and Bob sometimes play the
 * role of sender and sometimes play the role of receiver.
 */
// TODO: Once the PING protocol is specified via RFC, extract the PING tests into an abstract super-class. Every IT
//  should exercise PING functionality as a baseline, but both BTP and ILP-over-HTTP duplicate the same PING tests.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category( {IlpOverHttp.class})
public class TwoConnectorIlpOverHttpPingTestIT extends AbstractIlpOverHttpIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorIlpOverHttpPingTestIT.class);
  private static final Network network = Network.newNetwork();
  private static Topology topology = TwoConnectorPeerIlpOverHttpTopology.init();
  private static GenericContainer redis = ContainerHelper.redis(network);
  private static GenericContainer postgres = ContainerHelper.postgres(network);
  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  @BeforeClass
  public static void startTopology() {
    LOGGER.info("Starting test topology `{}`...", topology.toString());
    redis.start();
    postgres.start();
    topology.start();
    LOGGER.info("Test topology `{}` started!", topology.toString());
  }

  @AfterClass
  public static void stopTopology() {
    LOGGER.info("Stopping test topology `{}`...", topology.toString());
    topology.stop();
    postgres.stop();
    redis.stop();
    LOGGER.info("Test topology `{}` stopped!", topology.toString());
  }

  @Before
  public void setUp() {
    aliceConnector = this.getILPv4NodeFromGraph(getAliceConnectorAddress());
    // Note Bob's Connector's address is purposefully a child of Alice due to IL-DCP
    bobConnector = this.getILPv4NodeFromGraph(getBobConnectorAddress());
    this.resetBalanceTracking();
  }

  @Override
  protected InterledgerAddress getAliceConnectorAddress() {
    return ALICE_CONNECTOR_ADDRESS;
  }

  @Override
  protected InterledgerAddress getBobConnectorAddress() {
    return BOB_CONNECTOR_ADDRESS;
  }

  @Test
  public void testAliceNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(getAliceConnectorAddress());
    assertThat(connector.getConnectorSettings().operatorAddress(), is(getAliceConnectorAddress()));

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), BOB_ACCOUNT);
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().tokenSubject(), is(ALICE));
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(ilpOverHttpLink.getLinkSettings().incomingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.JWT_HS_256));
  }

  @Test
  public void testBobNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(getBobConnectorAddress());
    assertThat(connector.getConnectorSettings().operatorAddress(), is(getBobConnectorAddress()));

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getBobConnectorAddress(), ALICE_ACCOUNT);
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().tokenSubject(), is(BOB));
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(ilpOverHttpLink.getLinkSettings().incomingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.JWT_HS_256));
  }

  /**
   * Pinging a non-connector account (i.e., yourself) is not allowed because ping is only meant to be used at a
   * Connector level. Thus, this should ping request should reject.
   */
  @Test
  public void testPaulPingsPaulChildAccount() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);
    final PingInitiator pingInitiator = new DefaultPingInitiator(ilpOverHttpLink, () -> Instant.now().plusSeconds(30));

    // Note Bob's Connector's address is purposefully a child of Alice due to IL-DCP

    final long start = System.currentTimeMillis();
    pingInitiator.ping(PAUL_AT_ALICE_ADDRESS, UnsignedLong.ONE).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s)", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
          assertThat(rejectPacket.getMessage(), is("Destination address is unreachable"));
          assertThat(rejectPacket.getTriggeredBy().isPresent(), is(true));
          assertThat(rejectPacket.getTriggeredBy().get(), is(getAliceConnectorAddress()));
          latch.countDown();
        }
    );

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /**
   * Pinging a local connector account (i.e., Paul to his own Connector) is allowed.
   */
  @Test
  public void testPaulPingsAliceConnector() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getAliceConnectorAddress(), UnsignedLong.ONE);

    // test.alice.paul: Should be -1 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ONE.negate());
    // test.alice.bob: Should be 0 because this account is never engaged.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    // test.alice.__ping_account__: Should be +1 because this account fulfills and is actually receiving money.
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ONE);

    // test.alice.__ping_account__: Should be 1 since this account received the ping request.
    // NOTE: This test doesn't assert any other PING account balances because when Redis is running (e.g., in the IT)
    // then both Connectors share the same Redis instance, and this test breaks (i.e., the ITs only work with the
    // in-memory balance tracker)
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ONE);
  }

  /**
   * In this setup, the peering account that Alice uses to talk to Bob is used to ping Alice. Here, the outbound link
   * will place a ping packet on the link, sending it directly to Bob, who will try to route it back to Alice. However,
   * the next-hop routing logic will detect a routing loop, and reject the packet.
   */
  @Test
  public void testAlicePingsAliceUsingBobAccount() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), BOB_ACCOUNT);
    final PingInitiator pingInitiator = new DefaultPingInitiator(ilpOverHttpLink, () -> Instant.now().plusSeconds(30));

    final long start = System.currentTimeMillis();

    pingInitiator.ping(getAliceConnectorAddress(), UnsignedLong.ONE).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s)", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
          assertThat(rejectPacket.getMessage(), is("Destination address is unreachable"));
          assertThat(rejectPacket.getTriggeredBy().isPresent(), is(true));
          assertThat(rejectPacket.getTriggeredBy().get(), is(getBobConnectorAddress()));
          latch.countDown();
        }
    );

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }

  /**
   * Paul (a child account of Alice) pings Bob via Alice, which should succeed using 0 units.
   */
  @Test
  public void testPaulPingsBobConnectorWith0Units() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.ZERO);

    // test.alice.paul: Should be 0 because the packet units are 0
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ZERO);
    // test.alice.bob: Should be 0 because the packet units are 0
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    // test.alice.__ping_account__: Should be 0 because the packet units are 0
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    // test.bob.__ping_account__: Untouched
    // NOTE: This test doesn't assert any other PING account balances because when Redis is running (e.g., in the IT)
    // then both Connectors share the same Redis instance, and this test breaks (i.e., the ITs only work with the
    // in-memory balance tracker)
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, ZERO);
  }

  /**
   * Paul (a child account of Alice) pings Bob via Alice, which should succeed using 1 units.
   */
  @Test
  public void testPaulPingsBobConnectorWith1Units() throws InterruptedException {
    // After the Ping, the balances should look like this:
    // [[PAUL]][1] <-> [-1][[ALICE]][1] <-> [-1][BOB][1] <-> [-1][[PING_ACT]]
    this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.ONE);

    // test.alice.paul: SHOULD BE -1 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ONE.negate());

    // test.alice.bob: SHOULD BE 1 because this account will get fulfilled and increment by 1.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ONE);

    // test.bob.alice: SHOULD BE -1 because it prepares on Bob.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ONE.negate());
    // test.bob.__ping_account__: SHOULD BE +1 because it's receiving the ping funds.
    // NOTE: This test doesn't assert any other PING account balances because when Redis is running (e.g., in the IT)
    // then both Connectors share the same Redis instance, and this test breaks (i.e., the ITs only work with the
    // in-memory balance tracker)
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, ONE);
  }

  /**
   * Paul (a child account of Alice) pings Bob via Alice, which should succeed using 10 units.
   */
  @Test
  public void testPaulPingsBobWith10Units() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.valueOf(10));

    // test.alice.paul: Should be -10 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, TEN.negate());
    // test.alice.bob: Should be 10 because this account will receive ten from Paul on this Connector.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, TEN);

    // test.bob.alice: Should be -10 because it pays from Alice Connector, but pays one to the ping account on Bob.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, TEN.negate());
    // test.bob.__ping_account__: Should be +10 because it's receiving the ping funds.  Because this account is owned
    // by the Connector, it's OK to extend the 1 unit of credit above to the incoming account.
    // NOTE: This test doesn't assert any other PING account balances because when Redis is running (e.g., in the IT)
    // then both Connectors share the same Redis instance, and this test breaks (i.e., the ITs only work with the
    // in-memory balance tracker)
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, TEN);
  }

  /**
   * Random address should reject since it's not in the Bob's routing table.
   */
  @Test
  public void testPaulPingsRandom() throws InterruptedException {
    final InterledgerAddress randomDestination =
        InterledgerAddress.of(InterledgerAddressPrefix.TEST3.with(UUID.randomUUID().toString()).getValue());

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);
    final PingInitiator pingInitiator = new DefaultPingInitiator(ilpOverHttpLink, () -> Instant.now().plusSeconds(30));

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    pingInitiator.ping(randomDestination, UnsignedLong.ONE).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s)", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
          assertThat(rejectPacket.getMessage(), is("Destination address is unreachable"));
          assertThat(rejectPacket.getTriggeredBy().isPresent(), is(true));
          assertThat(rejectPacket.getTriggeredBy().get(), is(getAliceConnectorAddress()));
          latch.countDown();
        }
    );

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    LOGGER.info("Ping took {}ms", end - start);
  }


  /**
   * Random address should reject since it's not in the Bob's routing table.
   */
  @Test
  public void testPaulPingsRandomAtBob() throws InterruptedException {
    final InterledgerAddress randomDestination = getBobConnectorAddress().with(UUID.randomUUID().toString());

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);
    final PingInitiator pingInitiator = new DefaultPingInitiator(ilpOverHttpLink, () -> Instant.now().plusSeconds(30));

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    pingInitiator.ping(randomDestination, UnsignedLong.ONE).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s)", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
          assertThat(rejectPacket.getMessage(), is("Destination address is unreachable"));
          assertThat(rejectPacket.getTriggeredBy().isPresent(), is(true));
          assertThat(rejectPacket.getTriggeredBy().get(), is(getBobConnectorAddress()));
          latch.countDown();
        }
    );

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

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);
    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
        .executionCondition(PING_PROTOCOL_CONDITION)
        .expiresAt(Instant.now().minusSeconds(500))
        .amount(UnsignedLong.ONE) // Ping with the smallest unit...
        .destination(getBobConnectorAddress())
        .build();

    ilpOverHttpLink.sendPacket(pingPacket).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT));
          latch.countDown();
        }
    );

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

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
        .executionCondition(PING_PROTOCOL_CONDITION)
        .expiresAt(Instant.now().plusSeconds(30))
        .amount(UnsignedLong.valueOf(100000000000L)) // Ping with a unit that's too large...
        .destination(getBobConnectorAddress())
        .build();

    ilpOverHttpLink.sendPacket(pingPacket).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE));
          latch.countDown();
        }
    );

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

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
        .executionCondition(PING_PROTOCOL_CONDITION)
        .expiresAt(Instant.now().plusSeconds(30))
        .amount(UnsignedLong.valueOf(1L)) // Ping with a unit that's too large...
        .destination(InterledgerAddress.of("self.foo"))
        .build();

    ilpOverHttpLink.sendPacket(pingPacket).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
          latch.countDown();
        }
    );

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

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .expiresAt(Instant.now().plusSeconds(30))
        .amount(UnsignedLong.valueOf(1L)) // Ping with the smallest unit...
        .destination(getBobConnectorAddress())
        .build();

    ilpOverHttpLink.sendPacket(pingPacket).handle(
        fulfillPacket -> {
          fail(String.format("Ping request fulfilled, but should have rejected: %s", fulfillPacket));
          latch.countDown();
        }, rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
          latch.countDown();
        }
    );

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
  @Category(Performance.class)
  public void zTestPingPerf() throws InterruptedException {
    final IlpOverHttpLink paulToAliceLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), PAUL_ACCOUNT);
    final PingInitiator pingInitiator = new DefaultPingInitiator(paulToAliceLink, () -> Instant.now().plusSeconds(30));

    final int TIMEOUT = 30;
    final int numReps = 2000;

    final int numThreads = 5;
    final CountDownLatch latch = new CountDownLatch(numReps);

    // Tracks total time spent in the Runnable so we can find an average.
    AtomicInteger totalMillis = new AtomicInteger();

    final Runnable runnable = () -> {
      final long start = System.currentTimeMillis();

      pingInitiator.ping(getBobConnectorAddress(), UnsignedLong.ONE).handle(
          fulfillPacket -> {
            assertThat(fulfillPacket.getFulfillment().validateCondition(PING_PROTOCOL_CONDITION), is(true));
            latch.countDown();
          }, rejectPacket -> {
            fail(String.format("Ping request rejected, but should have fulfilled: %s", rejectPacket));
            latch.countDown();
          }
      );

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
        "averageProcessingTime should have been less than 30, but was " + averageProcessingTime,
        averageProcessingTime < 30,
        is(true)
    );
    assertThat(
        "averageMsPerPing should have been less than 6, but was " + averageMsPerPing, averageMsPerPing < 6,
        is(true)
    );

    // test.alice.paul: Should be -1 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, BigInteger.valueOf(numReps * -1));
    // test.alice.bob: Should be 0 because this account will receive one from Paul, but then pay the Bob Connector.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, BigInteger.valueOf(numReps));

    // test.bob.alice: Should be 0 because it gets `numReps` from Alice Connector, but pays `numReps` to the ping
    // account on Bob.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, BigInteger.valueOf(numReps * -1));
    // test.bob.__ping_account__: Should be `numReps` because it's receiving the ping funds.  Because this account is
    // owned by the Connector, it's OK to extend the 1 unit of credit above to the incoming account.
    // NOTE: This test doesn't assert any other PING account balances because when Redis is running (e.g., in the IT)
    // then both Connectors share the same Redis instance, and this test breaks (i.e., the ITs only work with the
    // in-memory balance tracker)
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
