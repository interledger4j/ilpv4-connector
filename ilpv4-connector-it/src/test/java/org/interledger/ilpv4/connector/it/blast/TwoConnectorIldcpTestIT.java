package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ConnectorProfiles;
import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.blast.BlastLink;
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
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.ALICE;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.ALICE_ADDRESS;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.BOB;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorParentChildBlastTopology.BOB_ADDRESS;
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
    System.setProperty(SPRING_PROFILES_ACTIVE, ConnectorProfiles.DEV);
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
      is(ALICE_ADDRESS)
    );

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().tokenSubject(), is(ALICE));
    assertThat(blastLink.getLinkSettings().incomingBlastLinkSettings().tokenSubject(), is(BOB));
  }

  @Test
  public void testBobNodeSettings() throws InterruptedException {
    // Give time for IL-DCP to work...
    Thread.sleep(2000);

    final ILPv4Connector connector = getILPv4NodeFromGraph(BOB_ADDRESS);
    assertThat(connector.getConnectorSettings().getOperatorAddress().isPresent(), is(true));
    assertThat(
      connector.getConnectorSettings().getOperatorAddress().get(),
      is(BOB_ADDRESS)
    );

    final BlastLink blastLink = getBlastLinkFromGraph(BOB_ADDRESS);
    assertThat(blastLink.getLinkSettings().outgoingBlastLinkSettings().tokenSubject(), is(BOB));
    assertThat(blastLink.getLinkSettings().incomingBlastLinkSettings().tokenSubject(), is(ALICE));
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
