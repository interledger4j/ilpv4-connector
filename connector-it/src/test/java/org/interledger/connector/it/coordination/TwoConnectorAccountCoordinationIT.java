package org.interledger.connector.it.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_HTTP_BASE_URL;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_AT_ALICE_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_HTTP_BASE_URL;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.it.AbstractIlpOverHttpIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.markers.Coordination;
import org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorClusterTopology;
import org.interledger.connector.it.topology.ClusteredTopology;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.LoopbackLink;

import okhttp3.HttpUrl;
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

import java.util.List;
import java.util.Optional;

/**
 * Tests to verify that two connectors running in a cluster coordinate on account events
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(Coordination.class)
@SuppressWarnings("UnstableApiUsage")
public class TwoConnectorAccountCoordinationIT extends AbstractIlpOverHttpIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorAccountCoordinationIT.class);
  private static final Network network = Network.newNetwork();
  private static ClusteredTopology topology = TwoConnectorClusterTopology.init();
  private static GenericContainer redis = ContainerHelper.redis(network);
  private static GenericContainer postgres = ContainerHelper.postgres(network);
  private ILPv4Connector alice1Connector;
  private ILPv4Connector alice2Connector;
  private ConnectorAdminClient alice1Client;
  private ConnectorAdminClient alice2Client;

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
    List<ConnectorServerNode> nodes = topology.getNodes(getAliceConnectorAddress(), ConnectorServerNode.class);
    if (nodes.size() != 2) {
      throw new IllegalStateException("Expected 2 nodes from topology but was " + nodes.size());
    }
    alice1Connector = nodes.get(0).getContentObject().getContext()
      .getBean(ILPv4Connector.class);
    alice2Connector = nodes.get(1).getContentObject().getContext()
      .getBean(ILPv4Connector.class);
    this.resetBalanceTracking();
    alice1Client = ConnectorAdminClient.construct(HttpUrl.get(ALICE_HTTP_BASE_URL), basicAuthRequestInterceptor());
    alice2Client = ConnectorAdminClient.construct(HttpUrl.get(BOB_HTTP_BASE_URL), basicAuthRequestInterceptor());
  }

  @Test
  public void accountUpdatedEventGoesAcrossConnectors() throws Exception {

    String accountId = "ricketycricket";
    Optional<AccountSettings> ricketycricket = alice1Client.findAccount(accountId);
    assertThat(ricketycricket).isEmpty();
    ricketycricket = alice2Client.findAccount(accountId);
    assertThat(ricketycricket).isEmpty();

    alice1Client.createAccount(AccountSettings.builder()
      .accountId(AccountId.of(accountId))
      .assetCode("XRP")
      .assetScale(0)
      .accountRelationship(AccountRelationship.PEER)
      .linkType(LoopbackLink.LINK_TYPE)
      .build());

    ricketycricket = alice1Client.findAccount(accountId);
    assertThat(ricketycricket).isNotEmpty().get().extracting("assetCode").isEqualTo("XRP");
    ricketycricket = alice2Client.findAccount(accountId);
    assertThat(ricketycricket).isNotEmpty().get().extracting("assetCode").isEqualTo("XRP");

    ricketycricket = alice1Connector.getAccountSettingsCache().getAccount(AccountId.of(accountId));
    assertThat(ricketycricket).isNotEmpty().get().extracting("assetCode").isEqualTo("XRP");
    ricketycricket = alice2Connector.getAccountSettingsCache().getAccount(AccountId.of(accountId));
    assertThat(ricketycricket).isNotEmpty().get().extracting("assetCode").isEqualTo("XRP");

    alice1Client.updateAccount(accountId, AccountSettings.builder()
      .accountId(AccountId.of(accountId))
      .assetCode("USD")
      .assetScale(0)
      .accountRelationship(AccountRelationship.PEER)
      .linkType(LoopbackLink.LINK_TYPE)
      .build());

    // give the event listener a little time to react
    Thread.sleep(2000);

    // since the cache should have been invalidated, both caches should return the updated version
    ricketycricket = alice1Connector.getAccountSettingsCache().getAccount(AccountId.of(accountId));
    assertThat(ricketycricket).isNotEmpty().get().extracting("assetCode").isEqualTo("USD");
    ricketycricket = alice2Connector.getAccountSettingsCache().getAccount(AccountId.of(accountId));
    assertThat(ricketycricket).isNotEmpty().get().extracting("assetCode").isEqualTo("USD");
  }

  @Override
  protected InterledgerAddress getAliceConnectorAddress() {
    return ALICE_CONNECTOR_ADDRESS;
  }

  @Override
  protected InterledgerAddress getBobConnectorAddress() {
    return BOB_AT_ALICE_ADDRESS;
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected ClusteredTopology getTopology() {
    return topology;
  }
}
