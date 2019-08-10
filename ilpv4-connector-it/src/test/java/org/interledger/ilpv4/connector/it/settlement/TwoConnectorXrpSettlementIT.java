package org.interledger.ilpv4.connector.it.settlement;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.AbstractBlastIT;
import org.interledger.ilpv4.connector.it.markers.Settlement;
import org.interledger.ilpv4.connector.it.topologies.settlement.SimulatedXrplSettlementTopology;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
import static java.math.BigInteger.ZERO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.BOB_CONNECTOR_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.AbstractTopology.PAUL_ACCOUNT;

/**
 * Tests to verify that two connectors can make settlement packets to each other using an XRP Ledger Settlement Engine.
 *
 * @see "https://github.com/interledgerjs/settlement-xrp"
 */
@Category(Settlement.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TwoConnectorXrpSettlementIT extends AbstractBlastIT {

  private static final BigInteger NINE_HUNDRED = BigInteger.valueOf(900L);
  private static final BigInteger THOUSAND = BigInteger.valueOf(1000L);

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorXrpSettlementIT.class);
  private static Topology topology = SimulatedXrplSettlementTopology.init();

  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  @BeforeClass
  public static void startTopology() {
    LOGGER.info("Starting test topology `{}`...", topology.toString());
    topology.start();
    LOGGER.info("Test topology `{}` started!", topology.toString());
  }

  @AfterClass
  public static void stopTopology() {
    LOGGER.info("Stopping test topology `{}`...", topology.toString());
    topology.stop();
    LOGGER.info("Test topology `{}` stopped!", topology.toString());
  }

  @Before
  public void setup() {
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
    final ILPv4Connector connector = getILPv4NodeFromGraph(getAliceConnectorAddress());
    assertThat(
      aliceConnector.getConnectorSettings().getOperatorAddress().get(),
      is(getAliceConnectorAddress())
    );

    assertThat(
      bobConnector.getConnectorSettings().getOperatorAddress().get(),
      is(getBobConnectorAddress())
    );

    // This is somewhat wonky because doing ping this way won't update the source balance properly. Thus, no balances
    // are asserted here.
    this.testPing(BOB_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), ZERO);
    this.testPing(ALICE_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(), ZERO);
  }

  /**
   * <p>This test validates that settlement is triggered by Alice once her balance with Bob exceeds the settlement
   * threshold (which is 10). To do this, Alice will ping Bob 10 times. If these packets fulfill, then Alice will see
   * her balance with Bob go up (and Bob will see his balance with Alice go down).</p>
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
      this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), new BigInteger("100"));
    }

    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, NINE_HUNDRED.negate());
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, NINE_HUNDRED);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, NINE_HUNDRED.negate());
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, NINE_HUNDRED);

    // Use the `paul` account on ALICE to ping BOB 1 more time, which should trigger settlmeent.
    this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), new BigInteger("100"));

    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, THOUSAND.negate());
    // This amount is ZERO because the onFulfill script will preemptiely reduce this account by the settlement amount.
    // In this test, the settle threshold is 1000, and settle_to is 0, so the new balance will be 0 because we expect
    // a settlement payment to be made (note that on an exception, this amount should go back up).
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO);
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, THOUSAND.negate());
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, THOUSAND);

    // Wait for Settlement to be triggered (XRPL closes a ledger every 3-4 seconds, so we probably need at least 10
    // seconds just to be safe).
    Thread.sleep(20000);

    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, THOUSAND.negate());
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, ZERO); // this amount was pre-emptively set to 0.
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, ZERO); // If settlement is successful, this should be 0 too.
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, THOUSAND);
  }

  // TODO: Finish this test per https://github.com/sappenin/java-ilpv4-connector/projects/5

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
