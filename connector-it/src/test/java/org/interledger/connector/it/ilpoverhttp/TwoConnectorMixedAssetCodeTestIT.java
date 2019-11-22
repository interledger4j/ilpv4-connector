package org.interledger.connector.it.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL_ACCOUNT;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.AbstractIlpOverHttpIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.markers.IlpOverHttp;
import org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology;
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

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(IlpOverHttp.class)
public class TwoConnectorMixedAssetCodeTestIT extends AbstractIlpOverHttpIT {

  private static final Denomination MICROCENTS_EUR = Denomination.builder()
      .assetCode("EUR")
      .assetScale((short) 8)
      .build();

  private static final Denomination NANO_YEN = Denomination.builder()
      .assetCode("JPY")
      .assetScale((short) 9)
      .build();

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorMixedAssetCodeTestIT.class);
  private static final Network network = Network.newNetwork();
  private static Topology topology = TwoConnectorPeerIlpOverHttpTopology.init(
      NANO_YEN, MICROCENTS_EUR, UnsignedLong.valueOf(1000000000L)
  );
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
  public void setUp() throws IOException, TimeoutException {
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
    BigInteger bobBalance = aliceConnector.getBalanceTracker().balance(BOB_ACCOUNT).netBalance();
    assertThat(bobBalance).isBetween(BigInteger.valueOf(115000000), BigInteger.valueOf(125000000));

    // test.bob.alice: Should be negative some range of the source because it pays from Alice Connector, but pays one
    // to the ping account on Bob.
    assertThat(bobConnector.getBalanceTracker().balance(ALICE_ACCOUNT).netBalance()).isEqualTo(bobBalance.negate());
    assertThat(bobConnector.getBalanceTracker().balance(PING_ACCOUNT_ID).netBalance()).isEqualTo(bobBalance);

    await().atMost(5, TimeUnit.SECONDS).until(() -> pubsubMessages.size() >= 2);

    assertThat(pubsubMessages).hasSize(2);
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

    assertThat(pubsubMessages).isEmpty();
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
