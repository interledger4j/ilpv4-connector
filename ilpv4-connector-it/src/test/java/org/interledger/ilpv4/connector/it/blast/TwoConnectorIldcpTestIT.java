package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.RuntimeProperties;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.ADMIN_PASSWORD;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.DEFAULT_JWT_TOKEN_ISSUER;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.DOT;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.SPRING_PROFILES_ACTIVE;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.ilpv4.connector.it.topologies.blast.AbstractTopology.ALICE_ACCOUNT;
import static org.interledger.ilpv4.connector.it.topologies.blast.AbstractTopology.BOB_ACCOUNT;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.ALICE;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.ALICE_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.BOB;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.BOB_AT_ALICE_ADDRESS;
import static org.junit.Assert.assertThat;

/**
 * Tests to verify that a single connector can become a child connector of a parent connector running IL-DCP. In this
 * IT, the Bob connector is a child of Alice.
 */
public class TwoConnectorIldcpTestIT extends AbstractBlastIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorIldcpTestIT.class);
  private static Topology topology = TwoConnectorParentChildBlastTopology.init();
  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  @BeforeClass
  public static void setupClass() {
    System.setProperty(DEFAULT_JWT_TOKEN_ISSUER, "https://connie.example.com");
    System.setProperty(ADMIN_PASSWORD, "password");
    System.setProperty(SPRING_PROFILES_ACTIVE, RuntimeProperties.ConnectorProfiles.DEV);
    // Required to get the conditional-config to work for this topology...
    System.setProperty(ConnectorProperties.ENABLED_PROTOCOLS + DOT + ConnectorProperties.BLAST_ENABLED, "true");

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
    bobConnector = this.getILPv4NodeFromGraph(BOB_AT_ALICE_ADDRESS);

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

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS, BOB_ACCOUNT);
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().tokenSubject(), is(ALICE));
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(blastLink.getLinkSettings().incomingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
  }

  @Test
  public void testBobNodeSettings() throws InterruptedException {
    // Give time for IL-DCP to work...
    Thread.sleep(2000);

    final ILPv4Connector connector = getILPv4NodeFromGraph(BOB_AT_ALICE_ADDRESS);
    assertThat(connector.getConnectorSettings().getOperatorAddress().isPresent(), is(true));
    assertThat(
      connector.getConnectorSettings().getOperatorAddress().get(),
      is(BOB_AT_ALICE_ADDRESS)
    );

    final BlastLink blastLink = getBlastLinkFromGraph(BOB_AT_ALICE_ADDRESS, ALICE_ACCOUNT);
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().tokenSubject(), is(BOB));
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(blastLink.getLinkSettings().incomingBlastLinkSettings().authType(),
      is(BlastLinkSettings.AuthType.JWT_HS_256));
  }

  /**
   * Alice and Bob are two connectors that have an account relationship with each other (alice is the parent, and Bob is
   * the child). In this test, the `bob` account on Alice is used by Alice to send a ping packet to Bob, which Bob then
   * fulfills because Ping is enabled.
   */
  @Test
  public void testAlicePingsBob() throws InterruptedException {
    this.testPing(ALICE_ADDRESS, BOB_ACCOUNT, BOB_AT_ALICE_ADDRESS);

    // ALICE
    assertAccountBalance(aliceConnector, AccountId.of(BOB), BigInteger.ZERO);

    // BOB
    assertAccountBalance(bobConnector, AccountId.of(ALICE), BigInteger.valueOf(1L));
  }

  /**
   * Alice and Bob are two connectors that have an account relationship with each other (alice is the parent, and Bob is
   * the child). In this test, the `alice` account on Bob is used by Bob to send a ping packet to Alice, which Alice
   * then fulfills because Ping is enabled.
   */
  @Test
  public void testBobPingsAlice() throws InterruptedException {
    this.testPing(BOB_AT_ALICE_ADDRESS, ALICE_ACCOUNT, ALICE_ADDRESS);

    // ALICE
    assertAccountBalance(aliceConnector, AccountId.of(BOB), BigInteger.valueOf(1L));

    // BOB
    assertAccountBalance(bobConnector, AccountId.of(ALICE), BigInteger.ZERO);
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
