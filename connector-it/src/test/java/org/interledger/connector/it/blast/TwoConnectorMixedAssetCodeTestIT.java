package org.interledger.connector.it.blast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL_ACCOUNT;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.AbstractBlastIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.markers.IlpOverHttp;
import org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerBlastTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.Denomination;

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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category( {IlpOverHttp.class})
public class TwoConnectorMixedAssetCodeTestIT extends AbstractBlastIT {

  private static final Denomination MICROCENTS_USD = Denomination.builder()
      .assetCode("USD")
      .assetScale((short) 8)
      .build();

  private static final Denomination MICROCENTS_EUR = Denomination.builder()
      .assetCode("EUR")
      .assetScale((short) 8)
      .build();

  private static final Denomination NANO_YEN = Denomination.builder()
      .assetCode("JPY")
      .assetScale((short) 9)
      .build();

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorMixedAssetCodeTestIT.class);
  private static Topology topology = TwoConnectorPeerBlastTopology.init(MICROCENTS_USD, NANO_YEN, MICROCENTS_EUR,
      1000000000L);

  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  private static final Network network = Network.newNetwork();

  private static GenericContainer redis = ContainerHelper.redis(network);

  private static GenericContainer postgres = ContainerHelper.postgres(network);

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

  /**
   * Paul (a child account of Alice) pings Bob via Alice, which should succeed using 10100000 units.
   */
  @Test
  public void sendAmountToBeConverted() throws InterruptedException {
    this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.valueOf(100000));

    // test.alice.paul: Should be -10000000 because that account initiated and paid for the ping.
    assertAccountBalance(aliceConnector, PAUL_ACCOUNT, BigInteger.valueOf(100000).negate());
    // test.alice.bob: Should be in range because this account will receive yen from Paul on this Connector.
    // hard to validate exchange rate but this should be close
    assertThat(aliceConnector.getBalanceTracker().balance(BOB_ACCOUNT).netBalance())
        .isBetween(BigInteger.valueOf(115000000), BigInteger.valueOf(125000000));

    // test.bob.alice: Should be negative some range of the source because it pays from Alice Connector, but pays one
    // to the ping account on Bob.
    assertThat(bobConnector.getBalanceTracker().balance(ALICE_ACCOUNT).netBalance())
        .isBetween(BigInteger.valueOf(125000000).negate(), BigInteger.valueOf(115000000).negate());
    assertThat(bobConnector.getBalanceTracker().balance(PING_ACCOUNT_ID).netBalance())
        .isBetween(BigInteger.valueOf(115000000), BigInteger.valueOf(125000000));
  }

  /**
   * Send an amount that will convert to more than the max packet amount on the other side
   */
  @Test
  public void sendTooMuch() throws InterruptedException {
    InterledgerResponsePacket response = this.testPing(PAUL_ACCOUNT, getAliceConnectorAddress(),
        getBobConnectorAddress(), UnsignedLong.valueOf(10000000), true);

    assertThat(response).isInstanceOf(InterledgerRejectPacket.class)
        .extracting("code").isEqualTo(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected Topology getTopology() {
    return topology;
  }

  @Override
  protected InterledgerAddress getAliceConnectorAddress() {
    return ALICE_CONNECTOR_ADDRESS;
  }

  @Override
  protected InterledgerAddress getBobConnectorAddress() {
    return BOB_CONNECTOR_ADDRESS;
  }
}
