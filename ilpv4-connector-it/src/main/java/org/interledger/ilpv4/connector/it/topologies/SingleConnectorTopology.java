package org.interledger.ilpv4.connector.it.topologies;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.model.settings.ImmutableAccountSettings;
import com.sappenin.ilpv4.plugins.btp.ws.ClientWebsocketBtpPlugin;
import com.sappenin.ilpv4.plugins.btp.ws.ServerWebsocketBtpPlugin;
import com.sappenin.ilpv4.server.ConnectorServer;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.edges.AccountEdge;
import org.interledger.plugin.lpiv2.ImmutablePluginSettings;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static com.sappenin.ilpv4.plugins.btp.BtpClientPluginSettings.*;
import static com.sappenin.ilpv4.plugins.btp.BtpPluginSettings.KEY_SECRET;
import static org.interledger.ilpv4.connector.it.topologies.TopologyUtils.defaultConnectorSettings;
import static org.interledger.ilpv4.connector.it.topologies.TopologyUtils.toServerNode;

/**
 * <p>A very simple graph that simulates BTP connections between a Sender (Alice) and a Receiver (Bob) who both
 * speak BTP to a Connector (Connie).
 *
 * <p>In this graph, Alice has a USD account with Connie (meaning Alice and Connie can owe each other USD).
 * Likewise with Connie and Bob. Alice can pay Bob by utilizing liquidity held by Connie.
 *
 * <p>Nodes in this graph are connected as follows:</p>
 *
 * <pre>
 *
 *           ┌────────────────────────────Setup──────────────────────────┐
 *           │                      ┌──────────────┐                     │
 *           │                      │              │                     ▼
 * ┌───────────────────┐            │              │           ┌──────────────────┐
 * │      SENDER       │            │  CONNECTOR   │           │     RECEIVER     │
 * │ test.connie.alice │───USD─────▷│ test.connie  │────USD───▶│ test.connie.bob  │
 * └───────────────────┘            │              │           └──────────────────┘
 *                                  │              │
 *                                  └──────────────┘
 * </pre>
 */
public class SingleConnectorTopology {

  public static final String USD = "USD";
  public static final InterledgerAddress CONNIE = InterledgerAddress.of("test.connie");
  public static final InterledgerAddress ALICE = CONNIE.with("alice");
  public static final InterledgerAddress BOB = CONNIE.with("bob");

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorTopology.class);

  static {
    // This is set to 0 so that the "port" value is used instead...
    System.setProperty("server.port", "0");
    System.setProperty("spring.jmx.enabled", "false");
    System.setProperty("spring.application.admin.enabled", "false");
  }

  public static Graph init() {

    // Some edges must be added _after_ the graph starts...
    final Graph graph = new Graph(new Graph.PostConstructListener() {
      @Override
      protected void doAfterGraphStartup(Graph g) {

        ////////////////////////
        // Add an Edge for Connie's view of Alice
        g.addEdge(
          new AccountEdge(CONNIE.getValue(), connieAccountForAlice(g))
        );

        ////////////////////////
        // Add an Edge for Connie's view of Bob
        // TODO

        ////////////////////////
        // Add an Edge for Alice's view of Connie
        //        g.addEdge(
        //          new AccountEdge(ALICE.getValue(), aliceAccountForConnie(g))
        //        );

        ////////////////////////
        // Add an Edge for Bob's view of Connie
        // TODO

      }
    });

    ///////////////////
    // Configure Alice
    ///////////////////

    ///////////////////
    // Configure Connie
    ///////////////////
    graph.addNode(CONNIE.getValue(), new ConnectorNode(new ConnectorServer(defaultConnectorSettings(CONNIE))));
    // This must be set in this fashion in order for the Websocket Server to enable...
    toServerNode(graph, CONNIE).setProperty(ConnectorSettings.PROPERTY_NAME__WEBSOCKETS_ENABLED, "true");

    ///////////////////
    // Configure Bob
    ///////////////////


    LOGGER.info("\n" +
      "\n1-CONNECTOR ARCHITECTURE" +
      "\n" +
      "                                                                                \n" +
      "          ┌────────────────────────────Setup──────────────────────────┐         \n" +
      "          │                      ┌──────────────┐                     │         \n" +
      "          │                      │              │                     ▼         \n" +
      "┌───────────────────┐            │              │           ┌──────────────────┐\n" +
      "│      SENDER       │            │  CONNECTOR   │           │     RECEIVER     │\n" +
      "│ test.connie.alice │───USD─────▷│ test.connie  │────USD───▶│ test.connie.bob  │\n" +
      "└───────────────────┘            │              │           └──────────────────┘\n" +
      "                                 │              │                               \n" +
      "                                 └──────────────┘                               \n"
    );
    return graph;
  }

  /**
   * Construct an {@link AccountSettings} object to represent Alice's account inside of Connie.
   */
  private static AccountSettings connieAccountForAlice(final Graph graph) {
    Objects.requireNonNull(graph);

    final Map<String, Object> customSettings = Maps.newConcurrentMap();
    customSettings.put("foo", "bar");
    customSettings.put(KEY_SECRET, "shh");

    final PluginSettings pluginSettings = ImmutablePluginSettings.builder()
      // Connie is the BTP Server!
      .pluginType(ServerWebsocketBtpPlugin.PLUGIN_TYPE)
      .peerAccountAddress(ALICE)
      .localNodeAddress(CONNIE)
      .customSettings(customSettings)
      .build();

    return ImmutableAccountSettings.builder()
      .interledgerAddress(ALICE)
      .assetCode(USD)
      .relationship(IlpRelationship.PARENT) // Connie is the PARENT of Alice.
      .pluginSettings(pluginSettings)
      .build();
  }


  /**
   * Construct an {@link AccountSettings} object to represent Alice's account inside of Connie.
   */
  private static AccountSettings aliceAccountForConnie(final Graph graph) {
    Objects.requireNonNull(graph);

    // Because Alice is configured as a BTPClient, we need to add the following fields to the CustomProperties section
    // in order for the plugin to be constructed properly.
    final Map<String, Object> customSettings = Maps.newConcurrentMap();
    customSettings.put("foo", "bar");
    customSettings.put(KEY_SECRET, "shh");
    customSettings.put(KEY_REMOTE_PEER_SCHEME, graph.getNode(CONNIE.getValue()).getScheme());
    customSettings.put(KEY_REMOTE_PEER_HOSTNAME, graph.getNode(CONNIE.getValue()).getHost());
    customSettings.put(KEY_REMOTE_PEER_PORT, graph.getNode(CONNIE.getValue()).getPort());

    final PluginSettings pluginSettings = ImmutablePluginSettings.builder()
      // Alice is the BTP Client!
      .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
      .peerAccountAddress(CONNIE)
      .localNodeAddress(ALICE)
      .customSettings(customSettings)
      .build();

    return ImmutableAccountSettings.builder()
      .interledgerAddress(CONNIE)
      .assetCode(USD)
      .relationship(IlpRelationship.CHILD) // Connie is the PARENT of Alice.
      .pluginSettings(pluginSettings)
      .build();
  }

}
