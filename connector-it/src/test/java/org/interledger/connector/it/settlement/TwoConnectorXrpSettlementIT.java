package org.interledger.connector.it.settlement;

import static java.math.BigInteger.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.PETER_ACCOUNT;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.events.IncomingSettlementSucceededEvent;
import org.interledger.connector.it.AbstractIlpOverHttpIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.markers.Settlement;
import org.interledger.connector.it.topologies.settlement.SimulatedXrplSettlementTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.core.InterledgerAddress;

import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Tests to verify that two connectors can make settlement packets to each other using an XRP Ledger Settlement Engine.
 *
 * @see "https://github.com/interledgerjs/settlement-xrp"
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(Settlement.class)
@SuppressWarnings("UnstableApiUsage")
public class TwoConnectorXrpSettlementIT extends AbstractIlpOverHttpIT {

  private static final BigInteger ONE_HUNDRED = BigInteger.valueOf(100L);
  private static final BigInteger NINE_HUNDRED = BigInteger.valueOf(900L);
  private static final BigInteger THOUSAND = BigInteger.valueOf(1000L);

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorXrpSettlementIT.class);
  private static final Network network = Network.newNetwork();
  private static Topology topology;
  private static GenericContainer redis = ContainerHelper.redis(network);
  private static GenericContainer postgres = ContainerHelper.postgres(network);
  private static GenericContainer settlementAlice = ContainerHelper.settlement(network, 9000, 8080, LOGGER);
  private static GenericContainer settlementBob = ContainerHelper.settlement(network, 9001, 8081, LOGGER);
  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  @BeforeClass
  public static void startTopology() {

    redis.start();
    postgres.start();
    settlementAlice.start();
    settlementBob.start();
    topology = SimulatedXrplSettlementTopology.init(
        settlementAlice.getMappedPort(9000),
        settlementBob.getMappedPort(9001)
    );
    LOGGER.info("Starting test topology `{}`...", topology.toString());
    topology.start();
    LOGGER.info("Test topology `{}` started!", topology.toString());
  }

  @AfterClass
  public static void stopTopology() {
    LOGGER.info("Stopping test topology `{}`...", topology.toString());
    topology.stop();
    settlementAlice.stop();
    settlementBob.stop();
    postgres.stop();
    redis.stop();
    LOGGER.info("Test topology `{}` stopped!", topology.toString());
  }

  @Before
  public void setUp() {
    aliceConnector = this.getILPv4NodeFromGraph(getAliceConnectorAddress());
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
  public void testNodeSettings() throws InterruptedException {
    assertThat(aliceConnector.getConnectorSettings().operatorAddress()).isEqualTo(getAliceConnectorAddress());
    assertThat(bobConnector.getConnectorSettings().operatorAddress()).isEqualTo(getBobConnectorAddress());

    // This is somewhat wonky because doing ping this way won't update the source balance properly. Thus, no balances
    // are asserted here.
    this.testPing(BOB_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.ZERO);
    this.testPing(ALICE_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(), UnsignedLong.ZERO);
  }

  /**
   * <p>This test validates that settlement is triggered by Alice once her balance with Bob exceeds the settlement
   * threshold (which is 10). To do this, Paul will ping Bob (via Alice) 10 times. If these packets fulfill, then Alice
   * will see her balance with Bob go up (and Bob will see his balance with Alice go down).</p>
   *
   * <p>Once settlement is triggered, then the balance should go back down to 0.</p>
   */
  @Test
  public void testTriggerSettlementOnAlice() throws InterruptedException {
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, ZERO);

    // Use the `paul` account on ALICE to ping BOB 9 times to get the balance up to 1000 (1 XRP Drop). This will take
    // the balance from 0 to +1000 on `bob@ALICE`, and -1000 from the perspective of `alice@BOB`. There is technically
    // a receiver called "ping" who would have a balance of -1000 when everything is said and done, but the Connector
    // doesn't track this balance currently. The balance tracker from the perspective of the Connector will be +1000,
    // however, for the account called "ping", indicating that the Connector has received 1000 units with respect to
    // its ping account.
    for (int i = 0; i < 9; i++) {
      getLogger().info("Ping {} of 9", i + 1);
      this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.valueOf(100));
    }

    getLogger().info("Checking balances after 9 pings...");
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, NINE_HUNDRED.negate());
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, NINE_HUNDRED);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, NINE_HUNDRED.negate());
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, NINE_HUNDRED);

    // Use this latch to wait for the Connector to receive a SettlementEvent...
    final CountDownLatch latch = new CountDownLatch(1);
    final Consumer<IncomingSettlementSucceededEvent> settlementSucceededCallback =
        new Consumer<IncomingSettlementSucceededEvent>() {
          @Override
          @Subscribe
          public void accept(IncomingSettlementSucceededEvent localSettlementProcessedEvent) {
            getLogger().info("Alice's Settlement Received by Bob: {}", localSettlementProcessedEvent);
            latch.countDown();
          }
        };
    // Wait for Alice to receive the settlement...
    bobConnector.getEventBus().register(settlementSucceededCallback);

    // Use the `paul` account on ALICE to ping BOB 1 more time, which should trigger settlement.
    getLogger().info("Ping 10 of 10 (should trigger settlement)");
    this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.valueOf(100));

    getLogger().info("Pre-settlement balances checks...");
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, THOUSAND.negate());
    // This amount is ZERO because the onFulfill script will preemptively reduce this account by the settlement amount.
    // In this test, the settle threshold is 1000, and settle_to is 0, so the new balance will be 0 because we expect
    // a settlement payment to be made (note that on an exception, this amount should go back up).
    // TODO: See https://github.com/sappenin/java-ilpv4-connector/issues/216 for tracking
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, THOUSAND.negate());
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, THOUSAND);

    getLogger().info("Waiting up to 20 seconds for Settlement to be processed...");
    latch.await(20, TimeUnit.SECONDS);
    bobConnector.getEventBus().unregister(settlementSucceededCallback); // for cleanup...

    getLogger().info("Post-settlement balances checks...");
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, THOUSAND.negate());
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO); // this amount was preemptively set to 0.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // If settlement is successful, this should be 0 too.
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, THOUSAND);
  }

  /**
   * <p>This test validates that settlement is triggered by Bob once his balance with Alice exceeds the settlement
   * threshold (which is 10). To do this, Peter will ping Alice 10 times (via Bob). If these packets fulfill, then Bob
   * will see his balance with Alice go up (and Alice will see her balance with Bob go down).</p>
   *
   * <p>Once settlement is triggered, then the balance should go back down to 0.</p>
   */
  @Test
  public void testTriggerSettlementOnBob() throws InterruptedException {
    assertAccountBalance(bobConnector, PETER_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    // Use the `peter` account on BOB to ping ALICE 9 times to get the balance up to 1000 (1 XRP Drop). This will take
    // the balance from 0 to +1000 on `alice@BOB`, and -1000 from the perspective of `bob@ALICE`. There is technically
    // a receiver called "ping" who would have a balance of -1000 when everything is said and done, but the Connector
    // doesn't track this balance currently. The balance tracker from the perspective of the Connector will be +1000,
    // however, for the account called "ping", indicating that the Connector has received 1000 units with respect to
    // its ping account.
    for (int i = 0; i < 9; i++) {
      getLogger().info("Ping {} of {}", i + 1, 9);
      this.testPing(PETER_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(), UnsignedLong.valueOf(100));
    }

    getLogger().info("Checking balances after 9 pings...");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, NINE_HUNDRED.negate());
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, NINE_HUNDRED);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, NINE_HUNDRED.negate());
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, NINE_HUNDRED);

    // Use this latch to wait for the Connector to receive a SettlementEvent...
    final CountDownLatch latch = new CountDownLatch(1);
    final Consumer<IncomingSettlementSucceededEvent> settlementSucceededCallback =
        new Consumer<IncomingSettlementSucceededEvent>() {
          @Override
          @Subscribe
          public void accept(IncomingSettlementSucceededEvent localSettlementProcessedEvent) {
            getLogger().info("Bob's Settlement Received by Alice: {}", localSettlementProcessedEvent);
            latch.countDown();
          }
        };
    // Wait for Alice to receive the settlement...
    aliceConnector.getEventBus().register(settlementSucceededCallback);

    // Use the `peter` account on ALICE to ping BOB 1 more time, which should trigger settlement.
    getLogger().info("Ping 10 of 10 (should trigger settlement)");
    this.testPing(PETER_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(), UnsignedLong.valueOf(100));

    getLogger().info("Pre-settlement balances checks...");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, THOUSAND.negate());
    // This amount is ZERO because the onFulfill script will preemptively reduce this account by the settlement amount.
    // In this test, the settle threshold is 1000, and settle_to is 0, so the new balance will be 0 because we expect
    // a settlement payment to be made (note that on an exception, this amount should go back up).
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, THOUSAND.negate());
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, THOUSAND);

    getLogger().info("Waiting up to 20 seconds for Settlement to be processed...");
    latch.await(20, TimeUnit.SECONDS);
    aliceConnector.getEventBus().unregister(settlementSucceededCallback); // for cleanup...

    getLogger().info("Post-settlement balances checks...");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, THOUSAND.negate());
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // If settlement is successful, this should be 0 too.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO); // this amount was pre-emptively set to 0.
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, THOUSAND);
  }

  /**
   * <p>This test validates that if settlement is triggered while more packets are flowing, that the balances will be
   * correct. To do this, Paul ping Alice until the settlement notification is encountered. The test asserts that the
   * balances are all correct for each participant.</p>
   */
  @Test
  public void testTriggerSettlementOnBobWithMorePacketsThanThreshold() throws InterruptedException {
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, ZERO);

    int totalPings = 19;

    // Use this latch to wait for the Connector to receive a SettlementEvent...
    final CountDownLatch latch = new CountDownLatch(1);
    final Consumer<IncomingSettlementSucceededEvent> settlementSucceededCallback =
        new Consumer<IncomingSettlementSucceededEvent>() {
          @Override
          @Subscribe
          public void accept(IncomingSettlementSucceededEvent localSettlementProcessedEvent) {
            getLogger().info("Alice's Settlement Received by Bob: {}", localSettlementProcessedEvent);
            latch.countDown();
          }
        };
    // Wait for Alice to receive the settlement...
    bobConnector.getEventBus().register(settlementSucceededCallback);

    // Ping 19 times, expecting settlement to be triggered at 10.
    for (int pingNumber = 1; pingNumber <= totalPings; pingNumber++) {
      getLogger().info("Ping {} of {}", pingNumber, totalPings);
      this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.valueOf(100));
    }

    getLogger().info("Waiting up to 20 seconds for Settlement to be processed...");
    latch.await(20, TimeUnit.SECONDS);
    bobConnector.getEventBus().unregister(settlementSucceededCallback); // for cleanup...

    getLogger().info("Post-settlement balances checks...");
    // -900
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, BigInteger.valueOf(totalPings).multiply(ONE_HUNDRED).negate());
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, NINE_HUNDRED); // 1900 - 1000 (settlement)
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, NINE_HUNDRED.negate()); // -1900 + 1000 (settlement)
    // 900
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, BigInteger.valueOf(totalPings).multiply(ONE_HUNDRED));
  }

  /**
   * <p>Named as such to enforce ordering it to execute last. plus it sounds cool.</p>
   *
   * <p>Currently ignored because we don't have the implementation set up to properly support a prefund. As currently
   * written, the test will fail on the last set of assertions with:</p>
   *
   * <p>java.lang.AssertionError: Incorrect balance for `bob` @ `org.interledger.connector.DefaultILPv4Connector@6442cf3e`!
   * Expected: is `0` but: was `1000`.</p>
   *
   * <p>This is because of the following conditions:
   *
   * <ol>
   *   <li>We pre-fund with 1000 by settling ahead of any prepares (this goes to Bob at Alice).</li>
   *   <li>We send pings to "use up" the 1000 balance (Peter at Bob pings to Ping at Alice, traversing the Alice/Bob line).</li>
   *   <li>Eventually we trigger settlement again, but we don't understand that the original settlement covered the balances.</li>
   *   <li>The balance goes back up to 1000 for Bob at Alice.</li>
   * </ol>
   * </p>
   *
   * <p>The last bit is undesirable because it effectively puts you in a state where you can never "use up" your
   * pre-fund and will always settle back to it. Since we don't have any kind of withdrawal mechanism either, this is problematic
   * because you will effectively be out of that money permanently.</p>
   *
   * <p>At some point in the future there will be more formal specifications to how this should be implemented, but as
   * of right now it's not practical to try and adjust the implementation to account for it.</p>
   */
  @Test
  @Ignore
  public void zealousPreemptiveSettlement() throws InterruptedException {
    assertAccountBalance(bobConnector, PETER_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    AccountSettings aliceAccountSettings = getAccountSettings(bobConnector.getConnectorSettings().operatorAddress(),
        ALICE_ACCOUNT);
    AccountSettings bobAccountSettings = getAccountSettings(aliceConnector.getConnectorSettings().operatorAddress(),
        BOB_ACCOUNT);

    getLogger().info("Alice settlement account id: {}, Bob settlement account id {}",
        aliceAccountSettings.settlementEngineDetails().get().settlementEngineAccountId().get().value(),
        bobAccountSettings.settlementEngineDetails().get().settlementEngineAccountId().get().value());

    bobConnector.getSettlementService().initiateLocalSettlement(UUID.randomUUID().toString(), aliceAccountSettings,
        SettlementQuantity.builder()
            .amount(THOUSAND)
            .scale(9)
            .build()
    );

    // Use this latch to wait for the Connector to receive a SettlementEvent...
    final CountDownLatch latch = new CountDownLatch(1);
    final Consumer<IncomingSettlementSucceededEvent> settlementSucceededCallback =
        new Consumer<IncomingSettlementSucceededEvent>() {
          @Override
          @Subscribe
          public void accept(IncomingSettlementSucceededEvent localSettlementProcessedEvent) {
            getLogger().info("Bob's Settlement Received by Alice: {}", localSettlementProcessedEvent);
            latch.countDown();
          }
        };
    // Wait for Alice to receive the settlement...
    aliceConnector.getEventBus().register(settlementSucceededCallback);

    getLogger().info("Waiting up to 20 seconds for Settlement to be processed...");
    latch.await(20, TimeUnit.SECONDS);
    aliceConnector.getEventBus().unregister(settlementSucceededCallback); // for cleanup...

    getLogger().info("Post-settlement balances checks...");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // Balance tracking isn't aware so balance is 0
    // (it should really be -1000)
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, THOUSAND); // this should have the credit from our prefund
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    this.testPing(PETER_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(),
        UnsignedLong.valueOf(NINE_HUNDRED));

    getLogger().info("Check balances after ping after (not enough to trigger settlement)");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, NINE_HUNDRED.negate());
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, NINE_HUNDRED); // What we owe (no settlement triggered)
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ONE_HUNDRED); // Prefund should be deducted
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, NINE_HUNDRED);

    // Use this latch to wait for the Connector to receive a SettlementEvent...
    final CountDownLatch finalCountdown = new CountDownLatch(1);
    final Consumer<IncomingSettlementSucceededEvent> finalSettlementSucceededCallback =
        new Consumer<IncomingSettlementSucceededEvent>() {
          @Override
          @Subscribe
          public void accept(IncomingSettlementSucceededEvent localSettlementProcessedEvent) {
            getLogger().info("Bob's Settlement Received by Alice: {}", localSettlementProcessedEvent);
            finalCountdown.countDown();
          }
        };
    // Wait for Alice to receive the settlement...
    aliceConnector.getEventBus().register(finalSettlementSucceededCallback);

    this
        .testPing(PETER_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(),
            UnsignedLong.valueOf(ONE_HUNDRED));

    getLogger().info("Waiting up to 20 seconds for Settlement to be processed...");
    finalCountdown.await(20, TimeUnit.SECONDS);
    aliceConnector.getEventBus().unregister(finalSettlementSucceededCallback); // for cleanup...

    getLogger().info("Check balances after ping after (settlement triggered; prefund used up)");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, THOUSAND.negate());
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // Should owe nothing after trigger
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO); // Prefund should be used up
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, THOUSAND);
  }

  @Test
  public void evenOutLeftoversFromSettlementsOutsidePrecision() throws InterruptedException {
    assertAccountBalance(bobConnector, PETER_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    BigInteger eleventyTen = BigInteger.valueOf(1100);
    // do this 10 times, meaning 11000 units total
    // this should trigger settlement 10x but will even out the leftovers of 100 units on each call that the
    // settlement engine is tracking
    for (int i = 0; i < 10; i++) {
      this.testPing(PETER_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(),
          UnsignedLong.valueOf(eleventyTen));
      final CountDownLatch latch = new CountDownLatch(1);
      final Consumer<IncomingSettlementSucceededEvent> settlementSucceededCallback =
          new Consumer<IncomingSettlementSucceededEvent>() {
            @Override
            @Subscribe
            public void accept(IncomingSettlementSucceededEvent localSettlementProcessedEvent) {
              getLogger().info("Bob's Settlement Received by Alice: {}", localSettlementProcessedEvent);
              latch.countDown();
            }
          };
      // Wait for Alice to receive the settlement...
      aliceConnector.getEventBus().register(settlementSucceededCallback);
      getLogger().info("Waiting up to 20 seconds for Settlement to be processed...");
      latch.await(20, TimeUnit.SECONDS);
      aliceConnector.getEventBus().unregister(settlementSucceededCallback); // for cleanup...

      if (i < 9) {
        BigInteger iteration = BigInteger.valueOf(i + 1);
        assertAccountBalance(bobConnector, PETER_ACCOUNT, eleventyTen.multiply(iteration).negate());
        assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // If settlement is successful, this should be 0 too.
        assertAccountBalance(aliceConnector, BOB_ACCOUNT, ONE_HUNDRED.multiply(iteration).negate());
        assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, eleventyTen.multiply(iteration));
      }
    }

    getLogger().info("Post-settlement balances checks...");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, eleventyTen.multiply(BigInteger.TEN).negate());
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // If settlement is successful, this should be 0 too.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO); // this amount was pre-emptively set to 0.
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, eleventyTen.multiply(BigInteger.TEN));

  }


  @Test
  public void increaseAccountBalanceBeyondSettleThreshold() throws InterruptedException {
    assertAccountBalance(bobConnector, PETER_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, ZERO);

    // Use this latch to wait for the Connector to receive a SettlementEvent...
    final CountDownLatch latch = new CountDownLatch(1);
    final Consumer<IncomingSettlementSucceededEvent> settlementSucceededCallback =
        new Consumer<IncomingSettlementSucceededEvent>() {
          @Override
          @Subscribe
          public void accept(IncomingSettlementSucceededEvent localSettlementProcessedEvent) {
            getLogger().info("Bob's Settlement Received by Alice: {}", localSettlementProcessedEvent);
            latch.countDown();
          }
        };
    // Wait for Alice to receive the settlement...
    aliceConnector.getEventBus().register(settlementSucceededCallback);

    BigInteger eleventyTen = BigInteger.valueOf(1100);
    this.testPing(PETER_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(),
        UnsignedLong.valueOf(eleventyTen));

    assertAccountBalance(bobConnector, PETER_ACCOUNT, eleventyTen.negate());
    // this is zero because...
    // - we crossed the settle threshold of 1000
    // - we settle to zero
    // one thing worth keeping in mind is that the settlement engine will track the 100 difference due to it
    // being below precision
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO);
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, eleventyTen.negate());
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, eleventyTen);

    getLogger().info("Waiting up to 20 seconds for Settlement to be processed...");
    latch.await(20, TimeUnit.SECONDS);
    aliceConnector.getEventBus().unregister(settlementSucceededCallback); // for cleanup...

    getLogger().info("Post-settlement balances checks...");
    assertAccountBalance(bobConnector, PETER_ACCOUNT, eleventyTen.negate());
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // If settlement is successful, this should be 0 too.
    // while the ALICE_ACCOUNT amount is zero, this will be -100
    // where is the missing 100 you might ask?
    // the settlement engine ends up tracking this as a leftover due to it below below its precision floor.
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ONE_HUNDRED.negate());
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, eleventyTen);

  }

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
