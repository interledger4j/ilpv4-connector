package org.interledger.connector.it.topologies.ilpoverhttp;

import org.interledger.connector.it.topologies.AbstractTopology;
import org.interledger.connector.it.topology.AbstractBaseTopology;
import org.interledger.connector.it.topology.ClusteredTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledFeatureSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.InterledgerAddressPrefix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>A very simple topology that simulates two connectors as they may exist in a cluster behind a theoretical
 * load balancer. Either connector should be able to handle requests by virtue of talking to the same database
 * and redis instance as the other. This is used for testing coordination events that are received by both
 * connectors to make sure data a change on Alice that triggers an event is handled by Bob.</p>
 *
 * <p>Nodes in this topology are not connected to each other directly though they share the same database
 * state:</p>
 *
 * <pre>
 * ┌──────────────┐        ┌──────────────┐
 * │              │        │              │
 * │              │        │              │
 * │  CONNECTOR   │        │  CONNECTOR   │
 * │  test.alice  │        │  test.alice  │
 * │              │        │              │
 * │              │        │              │
 * └──────────────┘        └──────────────┘
 * </pre>
 */
public class TwoConnectorClusterTopology extends AbstractTopology {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorClusterTopology.class);

  /**
   * In this topology, each Connector starts-up with no explicit knowledge of the other.
   *
   * @return The {@link Topology} of accounts and connectors
   */
  public static ClusteredTopology init() {
    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
    final ClusteredTopology topology = new ClusteredTopology(TwoConnectorClusterTopology.class.getSimpleName(),
      new AbstractBaseTopology.PostConstructListener<ClusteredTopology>() {
        @Override
        protected void doAfterTopologyStartup(ClusteredTopology g) {
          final List<ConnectorServerNode> aliceServerNodes =
            g.getNodes(ALICE_CONNECTOR_ADDRESS.getValue(), ConnectorServerNode.class);

          // Delete all accounts before initializing the Topology otherwise we see sporadic CI build failures when
          // building on Postgres. This includes the "ping" account so that ping balances get reset from Topology to
          // Topology.
          aliceServerNodes.stream().findFirst().orElseThrow(IllegalStateException::new)
            .getILPv4Connector().getAccountSettingsRepository().deleteAll();

        }
      });

    ///////////////////
    // Alice 1 Connector Node
    ///////////////////
    {
      final ConnectorServer alice1Server = new ConnectorServer(constructConnectorSettingsForAlice());
      alice1Server.setPort(ALICE_PORT);
      topology.addNode(ALICE_CONNECTOR_ADDRESS,
        new ConnectorServerNode(ALICE, alice1Server));
    }

    ///////////////////
    // Alice 2 Connector Node
    ///////////////////
    {
      final ConnectorServer alice2Server = new ConnectorServer(constructConnectorSettingsForAlice());
      alice2Server.setPort(BOB_PORT);
      topology.addNode(ALICE_CONNECTOR_ADDRESS,
        new ConnectorServerNode(ALICE, alice2Server));
    }

    LOGGER.info("\n" +
      "\nSTARTING ILP-OVER-HTTP TOPOLOGY\n" +
      "┌──────────────┐        ┌──────────────┐\n" +
      "│              │        │              │\n" +
      "│              │        │              │\n" +
      "│  CONNECTOR   │        │  CONNECTOR   │\n" +
      "│  test.alice  │        │  test.alice  │\n" +
      "│     (1)      │        │     (2)      │\n" +
      "│              │        │              │\n" +
      "└──────────────┘        └──────────────┘"
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

}
