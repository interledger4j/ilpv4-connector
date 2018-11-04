package org.interledger.ilpv4.connector.it.topologies;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.client.BtpWsClient;
import com.sappenin.ilpv4.client.IlpClient;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.model.settings.ImmutableAccountSettings;
import com.sappenin.ilpv4.server.ConnectorServer;
import org.interledger.btp.asn.framework.BtpCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.edges.ConnectorAccountEdge;
import org.interledger.ilpv4.connector.it.graph.edges.IlpSenderAccountEdge;
import org.interledger.ilpv4.connector.it.graph.nodes.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.nodes.IlpSenderNode;
import org.interledger.plugin.lpiv2.ImmutablePluginSettings;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.btp2.BtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.ServerWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Map;
import java.util.Objects;

import static org.interledger.ilpv4.connector.it.topologies.TopologyUtils.defaultConnectorSettings;
import static org.interledger.ilpv4.connector.it.topologies.TopologyUtils.toServerNode;
import static org.interledger.plugin.lpiv2.btp2.BtpClientPluginSettings.*;
import static org.interledger.plugin.lpiv2.btp2.BtpPluginSettings.KEY_SECRET;

/**
 * <p>A very simple graph that simulates BTP connections between a Sender (Alice) and a Receiver (Bob) who both
 * speak BTP to a Connector (Connie).
 *
 * <p>In this graph, Alice has a USD account with Connie (meaning Alice and Connie can owe each other USD).
 * Likewise, Connie and Bob have an EUR account (meaning Bob and Connie can owe each other EUR). Alice can pay Bob by
 * utilizing liquidity held by Connie.
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
 * │ test.connie.alice │───USD─────▷│ test.connie  │────EUR───▶│ test.connie.bob  │
 * └───────────────────┘            │              │           └──────────────────┘
 *                                  │              │
 *                                  └──────────────┘
 * </pre>
 */
public class SingleConnectorTopology {

  public static final String USD = "USD";
  public static final String EUR = "EUR";

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
          new ConnectorAccountEdge(CONNIE, aliceAccountInConnie(g))
        );

        ////////////////////////
        // Add an Edge for Connie's view of Bob
        // TODO

        ////////////////////////
        // Add an Edge for Alice's view of Connie
        g.addEdge(
          new IlpSenderAccountEdge(ALICE, connieAccountInAlice(g))
        );

        ////////////////////////
        // Add an Edge for Bob's view of Connie
        g.addEdge(
          new IlpSenderAccountEdge(BOB, connieAccountInBob(g))
        );


      }
    });

    ///////////////////
    // Configure Alice
    ///////////////////
    final IlpClient ilpSenderClient = new BtpWsClient();
    graph.addNode(ALICE, new IlpSenderNode(ilpSenderClient));

    ///////////////////
    // Configure Connie
    ///////////////////
    graph.addNode(CONNIE, new ConnectorNode(new ConnectorServer(defaultConnectorSettings(CONNIE))));
    // This must be set in this fashion in order for the Websocket Server to enable...
    toServerNode(graph, CONNIE).setProperty(ConnectorSettings.PROPERTY_NAME__WEBSOCKETS_ENABLED, "true");

    ///////////////////
    // Configure Bob
    ///////////////////
    final IlpClient ilpReceiverClient = new BtpWsClient();
    graph.addNode(BOB, new IlpSenderNode(ilpReceiverClient));

    LOGGER.info("\n" +
      "\nSTARTING 1-CONNECTOR TOPOLOGY" +
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
  private static AccountSettings aliceAccountInConnie(final Graph graph) {
    Objects.requireNonNull(graph);

    // Because Alice is configured as a BTPClient, we need to add the following fields to the CustomProperties section
    // in order for the plugin to be constructed properly.
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
  private static Plugin connieAccountInAlice(final Graph graph) {
    Objects.requireNonNull(graph);

    return constructBtpWsClientPlugin(graph, ALICE, CONNIE);
  }

  /**
   * Construct an {@link AccountSettings} object to represent Alice's account inside of Connie.
   */
  private static Plugin connieAccountInBob(final Graph graph) {
    Objects.requireNonNull(graph);

    return constructBtpWsClientPlugin(graph, BOB, CONNIE);
  }

  /**
   * Construct an {@link AccountSettings} object to represent Alice's account inside of Connie.
   */
  private static Plugin constructBtpWsClientPlugin(
    final Graph graph,
    final InterledgerAddress operatorAddress,
    final InterledgerAddress remoteNodeAddress
  ) {
    Objects.requireNonNull(graph);

    // Because Alice is configured as a BTPClient, we need to add the following fields to the CustomProperties section
    // in order for the plugin to be constructed properly.
    final Map<String, Object> customSettings = Maps.newConcurrentMap();
    customSettings.put("foo", "bar");
    customSettings.put(KEY_SECRET, "shh");
    customSettings.put(KEY_REMOTE_PEER_SCHEME, graph.getNodeAsServer(remoteNodeAddress).getScheme());
    customSettings.put(KEY_REMOTE_PEER_HOSTNAME, graph.getNodeAsServer(remoteNodeAddress).getHost());
    customSettings.put(KEY_REMOTE_PEER_PORT, graph.getNodeAsServer(remoteNodeAddress).getPort());

    final BtpClientPluginSettings pluginSettings = BtpClientPluginSettings.builder(customSettings)
      .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
      .peerAccountAddress(remoteNodeAddress)
      .localNodeAddress(operatorAddress)
      .build();

    final CodecContext ilpCodecContext = InterledgerCodecContextFactory.oer();
    final CodecContext btpCodecContext = BtpCodecContextFactory.oer();
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry = new BtpSubProtocolHandlerRegistry();
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter =
      new BinaryMessageToBtpPacketConverter(btpCodecContext);
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter =
      new BtpPacketToBinaryMessageConverter(btpCodecContext);
    final StandardWebSocketClient wsClient = new StandardWebSocketClient();

    return new ClientWebsocketBtpPlugin(
      pluginSettings, ilpCodecContext, btpCodecContext, btpSubProtocolHandlerRegistry,
      binaryMessageToBtpPacketConverter, btpPacketToBinaryMessageConverter, wsClient
    );
  }

}
