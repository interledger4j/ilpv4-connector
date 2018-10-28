package org.interledger.ilpv4.connector.it;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.model.settings.ImmutableAccountSettings;
import com.sappenin.ilpv4.model.settings.ImmutableConnectorSettings;
import com.sappenin.ilpv4.plugins.btp.ws.ClientWebsocketBtpPlugin;
import com.sappenin.ilpv4.plugins.btp.ws.ServerWebsocketBtpPlugin;
import com.sappenin.ilpv4.server.ConnectorServer;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.edges.AccountEdge;
import org.interledger.plugin.lpiv2.ImmutablePluginSettings;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sappenin.ilpv4.plugins.btp.BtpClientPluginSettings.*;
import static com.sappenin.ilpv4.plugins.btp.BtpPluginSettings.KEY_SECRET;

/**
 * <p>Starts a single ILPv4 Connector node, and attempts to communicate with an already running remote connector (e.g.,
 * a JS or other production connector).</p>
 *
 * <p>This test speaks BTP to the remote connector, and tests various things such as routing and core BTP
 * functionality.</p>
 *
 *
 * <p>This test is useful to validate existing functionality against a production
 * connector that isn't running inside of any test-harness-architecture as configured in this test.</p>
 */
public class SingleBtpClientServerTestArchitecture {

  // A parent node for this node...
  static final InterledgerAddress PARENT = InterledgerAddress.of("test.parent.up");
  // A peer node for this node...
  static final InterledgerAddress PEER = InterledgerAddress.of("test.peer");
  static final InterledgerAddress ALICE = InterledgerAddress.of("test.alice");

  static final String XRP = "XRP";

  static {
    System.setProperty("server.port", "0");
    System.setProperty("spring.jmx.enabled", "false");
    System.setProperty("spring.application.admin.enabled", "false");
  }

  /**
   * <p>A very simple graph that simulates BTP connections between Alice (the BTP Client) and Bob (the BTP Server).
   *
   * <p>In this graph, Alice has a USD account with Bob (meaning Alice and Bob can owe each other any type
   * of USD). Alice and Bob exchange BTP packets with each other.
   *
   * <p>Nodes in this graph are connected as follows:</p>
   *
   * <pre>
   * ┌──────────────┐           ┌──────────────┐
   * │              │           │              │
   * │  test.alice  │◁──XRP ───▷│   test.bob   │
   * │    (Java)    │           │     (JS)     │
   * └──────────────┘           └──────────────┘
   * </pre>
   */
  static Graph singleNodeGraph() {
    return new Graph(new Graph.PostConstructListener() {
      @Override
      protected void doAfterGraphStartup(Graph graph) {
        accountsOfAlice(graph).stream()
          .forEach(accountSettings -> graph.addEdge(new AccountEdge(ALICE.getValue(), accountSettings)));
      }
    })
      // Nodes
      .addNode(ALICE.getValue(), new ConnectorNode(new ConnectorServer(defaultConnectorSettings(ALICE))));
  }

  /**
   * In this Architecture, Alice accounts with Bob.
   */
  private static List<AccountSettings> accountsOfAlice(final Graph graph) {
    Objects.requireNonNull(graph);

    /////////////////
    // Local Accounts
    // NOTE: Ordinarily, these accounts would be configured using a BTP Server plugin, and sending would be performed
    // via an ILP-Sender to BTP connection. However, for this test, we reach into the Connector directly for all
    // "send" operations, so we can mock the plugin for her.
    /////////////////
    final Map<String, Object> aliceCustomSettings = Maps.newConcurrentMap();
    // TODO: Define a client vs server plugin...e.g., "client" should mean, "accepts connections" and server should
    // mean, "talks to a server"
    aliceCustomSettings.put(KEY_SECRET, "shh");
    //aliceCustomSettings.put(KEY_REMOTE_PEER_SCHEME, "ws");
    //aliceCustomSettings.put(KEY_REMOTE_PEER_HOSTNAME, "localhost");
    //aliceCustomSettings.put(KEY_REMOTE_PEER_PORT, "6666");
    final PluginSettings alicePluginSettings = ImmutablePluginSettings.builder()
      // Alice is the BTP Server!
      .pluginType(ServerWebsocketBtpPlugin.PLUGIN_TYPE)
      .peerAccountAddress(ALICE)
      .localNodeAddress(ALICE)
      .customSettings(aliceCustomSettings)
      .build();
    final AccountSettings aliceAccount = ImmutableAccountSettings.builder()
      .interledgerAddress(ALICE)
      .relationship(IlpRelationship.CHILD)
      .assetCode(XRP)
      .pluginSettings(alicePluginSettings)
      .build();

    /////////////////
    // Remote Accounts
    final Map<String, Object> parentCustomSettings = Maps.newConcurrentMap();
    parentCustomSettings.put("foo", "bar");
    parentCustomSettings.put(KEY_SECRET, "shh");
    parentCustomSettings.put(KEY_REMOTE_PEER_SCHEME, "ws");
    parentCustomSettings.put(KEY_REMOTE_PEER_HOSTNAME, "localhost");
    parentCustomSettings.put(KEY_REMOTE_PEER_PORT, "6666");

    final PluginSettings bobPluginSettings = ImmutablePluginSettings.builder()
      // Alice is the BTP Server!
      .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
      .peerAccountAddress(PARENT)
      .localNodeAddress(ALICE)
      .customSettings(parentCustomSettings)
      .build();

    // Peer with Parent
    final AccountSettings accountSettings = accountSettings(XRP, bobPluginSettings);
    return Lists.newArrayList(aliceAccount, accountSettings);
  }

  private static ConnectorSettings defaultConnectorSettings(final InterledgerAddress interledgerAddress) {
    return ImmutableConnectorSettings.builder()
      .ilpAddress(interledgerAddress)
      //.secret("secret")
      .build();
  }

  private static AccountSettings accountSettings(final String accountAssetCode, final PluginSettings pluginSettings
  ) {
    Objects.requireNonNull(accountAssetCode);
    Objects.requireNonNull(pluginSettings);

    return ImmutableAccountSettings.builder()
      .interledgerAddress(pluginSettings.getPeerAccountAddress())
      .assetCode(accountAssetCode)
      .pluginSettings(pluginSettings)
      .build();
  }

  /**
   * Configuration overrides for the <tt>test.alice</tt> server.
   */
  @SuppressWarnings("unused")
  private static final class ConnectorSettingsAlice {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ConnectorSettings connectorSettings() {
      return defaultConnectorSettings(ALICE);
    }
  }
}
