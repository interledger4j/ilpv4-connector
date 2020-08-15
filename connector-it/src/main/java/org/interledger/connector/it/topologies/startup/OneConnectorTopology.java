package org.interledger.connector.it.topologies.startup;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings.Builder;
import org.interledger.connector.it.topologies.AbstractTopology;
import org.interledger.connector.it.topology.AbstractBaseTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledFeatureSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.http.IlpOverHttpLink;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * <p>A single-server topology that communicates with a redis and postgrest same database. This topology is
 * generally targeted at the `ConnectorStartupTestIT`, but can be used with other test harnesses.</p>
 *
 * <pre>
 * ┌──────────────┐
 * │              │
 * │              │
 * │  CONNECTOR   │
 * │  test.alice  │
 * │              │
 * │              │
 * └──────────────┘
 * </pre>
 *
 * @see "https://github.com/interledger4j/ilpv4-connector/issues/666"
 */
public class OneConnectorTopology extends AbstractTopology {

  public static final int NUM_ACCOUNTS = 500;
  private static final Logger LOGGER = LoggerFactory.getLogger(OneConnectorTopology.class);

  /**
   * In this topology, each Connector starts-up with no explicit knowledge of the other.
   *
   * @return The {@link Topology} of accounts and connectors
   */
  public static Topology init() {
    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
    final Topology topology = new Topology(OneConnectorTopology.class.getSimpleName(),
      new AbstractBaseTopology.PostConstructListener<Topology>() {
        @Override
        protected void doAfterTopologyStartup(Topology g) {
          final ConnectorServerNode aliceServerNode =
            g.getNode(ALICE_CONNECTOR_ADDRESS.getValue(), ConnectorServerNode.class);

          // Delete all accounts before initializing the Topology otherwise we see sporadic CI build failures when
          // building on Postgres. This includes the "ping" account so that ping balances get reset from Topology to
          // Topology.
          if (aliceServerNode.getILPv4Connector().getAccountSettingsRepository().count() < 20) {
            LOGGER.info("About to create {} test accounts!", NUM_ACCOUNTS);
            for (int i = 0; i < NUM_ACCOUNTS; i++) {
              final AccountSettings accountSettings = constructAccountSettingsWithRoutingEnabled().build();
              aliceServerNode.getILPv4Connector().getAccountManager().createAccount(accountSettings);
            }
            LOGGER.info("Created {} test accounts!", NUM_ACCOUNTS);
          }
        }
      });

    ///////////////////
    // Alice 1 Connector Node
    ///////////////////

    final ConnectorServer aliceServer = new ConnectorServer(constructConnectorSettingsForAlice());
    aliceServer.setPort(ALICE_PORT);
    aliceServer.setProperty("logging.level.root", "INFO");
    aliceServer.setProperty("LOGGING_LEVEL", "INFO");
    // Use H2, but without all the extra overhead of useH2(...)
    aliceServer.setProperty("spring.profiles.include", "h2-in-memory, migrate");
    aliceServer.setProperty(
      "spring.datasource.url", "jdbc:h2:mem:connector-app;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    );
    aliceServer.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");

    topology.addNode(ALICE_CONNECTOR_ADDRESS, new ConnectorServerNode(ALICE, aliceServer));

    LOGGER.info("\n" +
      "\nSTARTING ONE-CONNECTOR TOPOLOGY\n" +
      "┌──────────────┐\n" +
      "│              │\n" +
      "│              │\n" +
      "│  CONNECTOR   │\n" +
      "│  test.alice  │\n" +
      "│     (1)      │\n" +
      "│              │\n" +
      "└──────────────┘"
    );
    return topology;
  }


  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Alice</tt>.
   */
  private static ConnectorSettings constructConnectorSettingsForAlice() {
    return ImmutableConnectorSettings.builder()
      .operatorAddress(ALICE_CONNECTOR_ADDRESS)
      .enabledFeatures(EnabledFeatureSettings.builder()
        .isRequire32ByteSharedSecrets(false)
        .build())
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isIlpOverHttpEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerRoutingEnabled(false)
        .isIldcpEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        // A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
        .build()
      )
      .build();
  }

  private static final Builder constructAccountSettingsWithRoutingEnabled() {
    return AccountSettings.builder()
      .accountId(AccountId.of(UUID.randomUUID().toString()))
      .assetCode("XRP")
      .assetScale(6)
      .accountRelationship(AccountRelationship.PEER)
      .isSendRoutes(true)
      .isReceiveRoutes(true)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(
        ImmutableMap.<String, String>builder()
          .put("ilpOverHttp.incoming.jwt.token_issuer", "https://xpring.io/portal/")
          .put("ilpOverHttp.outgoing.url", "https://localhost:9000/ilp")
          .put("ilpOverHttp.outgoing.simple.auth_token", "shh")
          .put("ilpOverHttp.outgoing.auth_type", "SIMPLE")
          .build()
      );
  }

}
