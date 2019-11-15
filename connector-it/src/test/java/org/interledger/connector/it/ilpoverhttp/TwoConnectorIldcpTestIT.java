package org.interledger.connector.it.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_AT_ALICE_ADDRESS;
import static org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorParentChildIlpOverHttpTopology.ALICE;
import static org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorParentChildIlpOverHttpTopology.BOB;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.AbstractIlpOverHttpIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.markers.IlpOverHttp;
import org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorParentChildIlpOverHttpTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;

import com.google.common.primitives.UnsignedLong;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.math.BigInteger;

/**
 * Tests to verify that a single connector can become a child connector of a parent connector running IL-DCP. In this
 * IT, the Bob connector is a child of Alice.
 */
@Category(IlpOverHttp.class)
public class TwoConnectorIldcpTestIT extends AbstractIlpOverHttpIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorIldcpTestIT.class);
  private static final Network network = Network.newNetwork();
  private static Topology topology = TwoConnectorParentChildIlpOverHttpTopology.init();
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
    redis.stop();
    postgres.stop();
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
    return BOB_AT_ALICE_ADDRESS;
  }

  @Test
  public void testAliceNodeSettings() {
    final ILPv4Connector connector = getILPv4NodeFromGraph(getAliceConnectorAddress());
    assertThat(connector.getConnectorSettings().operatorAddress()).isEqualTo(getAliceConnectorAddress());

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getAliceConnectorAddress(), BOB_ACCOUNT);
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().tokenSubject()).isEqualTo(ALICE);
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    assertThat(ilpOverHttpLink.getLinkSettings().incomingHttpLinkSettings().authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
  }

  @Test
  public void testBobNodeSettings() throws InterruptedException {
    // Give time for IL-DCP to work...
    Thread.sleep(2000);

    final ILPv4Connector connector = getILPv4NodeFromGraph(getBobConnectorAddress());
    assertThat(connector.getConnectorSettings().operatorAddress()).isEqualTo(getBobConnectorAddress());

    final IlpOverHttpLink ilpOverHttpLink = getIlpOverHttpLinkFromGraph(getBobConnectorAddress(), ALICE_ACCOUNT);
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().tokenSubject()).isEqualTo(BOB);
    assertThat(ilpOverHttpLink.getLinkSettings().outgoingHttpLinkSettings().authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    assertThat(ilpOverHttpLink.getLinkSettings().incomingHttpLinkSettings().authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
  }

  /**
   * Alice and Bob are two connectors that have an account relationship with each other (alice is the parent, and Bob is
   * the child). In this test, the `bob` account on Alice is used by Alice to send a ping packet to Bob, which Bob then
   * fulfills because Ping is enabled.
   */
  @Test
  public void testAlicePingsBob() throws InterruptedException {
    this.testPing(BOB_ACCOUNT, getAliceConnectorAddress(), getBobConnectorAddress(), UnsignedLong.ONE);

    // Bob@ALICE (this account is 0 because using it to ping does not engage any balance tracking).
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, BigInteger.valueOf(0L));

    // Alice@BOB
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, BigInteger.valueOf(-1L));

    // PING ACCOUNT (Note that currently this IT forces both Connectors to share the same Redis instance, so checking
    // Alice's PING_ACCOUNT will be the same as alice, but techincally that's incorrect so it's unasserted here).
    assertAccountBalance(bobConnector, PING_ACCOUNT_ID, BigInteger.valueOf(1L));
  }

  /**
   * Alice and Bob are two connectors that have an account relationship with each other (alice is the parent, and Bob is
   * the child). In this test, the `alice` account on Bob is used by Bob to send a ping packet to Alice, which Alice
   * then fulfills because Ping is enabled.
   */
  @Test
  public void testBobPingsAlice() throws InterruptedException {
    this.testPing(ALICE_ACCOUNT, getBobConnectorAddress(), getAliceConnectorAddress(), UnsignedLong.ONE);

    // bob@ALICE (this account is 0 because using it to ping does not engage any balance tracking).
    assertAccountBalance(bobConnector, ALICE_ACCOUNT, BigInteger.valueOf(0));

    // Alice@BOB
    assertAccountBalance(aliceConnector, BOB_ACCOUNT, BigInteger.valueOf(-1L));

    // PING ACCOUNT (Note that currently this IT forces both Connectors to share the same Redis instance, so checking
    // Bob's PING_ACCOUNT will be the same as alice).
    assertAccountBalance(aliceConnector, PING_ACCOUNT_ID, BigInteger.valueOf(1L));
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
