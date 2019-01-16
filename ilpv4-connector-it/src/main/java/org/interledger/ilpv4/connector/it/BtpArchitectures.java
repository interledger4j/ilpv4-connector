package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.topologies.btp.SingleConnectorMultiAccountBtpTopology;

/**
 * A topology and setup for simulating the "BeerCoin" token.
 *
 * @deprecated This will be replaced by discrete toplogies. See
 * {@link SingleConnectorMultiAccountBtpTopology} as an example.
 */
@Deprecated
public class BtpArchitectures {
  //
  //  public static final String USD = "USD";
  //  public static final InterledgerAddress CONNIE = InterledgerAddress.of("test.connie");
  //  public static final InterledgerAddress ALICE_AT_CONNIE = CONNIE.with("alice");
  //  public static final InterledgerAddress BOB_AT_CONNIE = CONNIE.with("bob");
  //
  //  private static final Logger LOGGER = LoggerFactory.getLogger(BtpArchitectures.class);
  //
  //  static {
  //    // This is set to 0 so that the "port" value is used instead...
  //    System.setProperty("server.port", "0");
  //    System.setProperty("spring.jmx.enabled", "false");
  //    System.setProperty("spring.application.admin.enabled", "false");
  //  }
  //
  //  /**
  //   * <p>A very simple topology that simulates BTP connections between a Sender (Alice) and a Receiver (Bob) who both
  //   * speak BTP to a Connector (Connie).
  //   *
  //   * <p>In this topology, Alice has a USD account with Connie (meaning Alice and Connie can owe each other USD).
  //   * Likewise with Connie and Bob. Alice can pay Bob by utilizing liquidity held by Connie.
  //   *
  //   * <p>Nodes in this topology are connected as follows:</p>
  //   *
  //   * <pre>
  //   *
  //   *           ┌────────────────────────────Setup──────────────────────────┐
  //   *           │                                                           │
  //   *           │                      ┌──────────────┐                     ▼
  //   * ┌───────────────────┐            │              │           ┌──────────────────┐
  //   * │      SENDER       │            │              │           │     RECEIVER     │
  //   * │ test.connie.alice │───USD─────▷│ test.connie  │────USD───▶│ test.connie.bob  │
  //   * └───────────────────┘            │              │           └──────────────────┘
  //   *                                  │              │
  //   *                                  └──────────────┘
  //   * </pre>
  //   */
  //  static Topology oneConnectorGraph() {
  //
  //    // Some edges must be added _after_ the topology starts...
  //    final Topology topology = new Topology(new Topology.PostConstructListener() {
  //      @Override
  //      protected void doAfterGraphStartup(Topology g) {
  //        // Edges
  //        // Add all of alice's accounts as an edge between Alice and each account.
  //        accountsOfAlice(g).stream()
  //          .forEach(accountSettings -> g.addEdge(new ConnectorAccountEdge(ALICE_AT_CONNIE.getValue(), accountSettings)));
  //
  //        // Add all of bob's accounts as an edge between Bob and each account.
  //        accountsOfBob(g).stream()
  //          .forEach(accountSettings -> g.addEdge(new ConnectorAccountEdge(BOB_AT_CONNIE.getValue(), accountSettings)));
  //      }
  //    });
  //
  //
  //    ///////////////////
  //    // Configure Alice
  //    ///////////////////
  //
  //    ///////////////////
  //    // Configure Connie
  //    ///////////////////
  //    topology.addNode(ALICE_AT_CONNIE.getValue(), new IlpConnectorNode(new ConnectorServer(defaultConnectorSettings(ALICE_AT_CONNIE))));
  //    // This must be set in this fashion in order for the Websocket Server to enable...
  //    toServerNode(topology, ALICE_AT_CONNIE).setProperty(ConnectorSettings.PROPERTY_NAME__WEBSOCKETS_ENABLED, "true");
  //
  //    ///////////////////
  //    // Configure Bob
  //    ///////////////////
  //
  //
  //    LOGGER.info("\n" +
  //      "\n1-CONNECTOR_MODE ARCHITECTURE" +
  //      "\n" +
  //      "            ┌────────────────────────────Setup──────────────────────────┐       \n" +
  //      "            │                                                           │       \n" +
  //      "            │                      ┌──────────────┐                     ▼       \n" +
  //      "┌───────────────────┐            │              │           ┌──────────────────┐\n" +
  //      "│      SENDER       │            │              │           │     RECEIVER     │\n" +
  //      "│ test.connie.alice │───USD─────▷│ test.connie  │────USD───▶│ test.connie.bob  │\n" +
  //      "└───────────────────┘            │              │           └──────────────────┘\n" +
  //      "                                 │              │                               \n" +
  //      "                                 └──────────────┘                               \n"
  //    );
  //    return topology;
  //  }
  //
  //  /**
  //   * <p>A very simple topology that simulates BTP connections between Alice (the BTP Server) and Bob (the BTP Client).
  //   *
  //   * <p>In this topology, Alice has a USD account with Bob (meaning Alice and Bob can owe each other any type
  //   * of USD). Alice and Bob exchange BTP packets with each other.
  //   *
  //   * <p>Nodes in this topology are connected as follows:</p>
  //   *
  //   * <pre>
  //   * ┌──────────────┐           ┌──────────────┐
  //   * │              │           │              │
  //   * │  test.alice  │◁──USD-───▷│  test.bob    │
  //   * │              │           │              │
  //   * └──────────────┘           └──────────────┘
  //   * </pre>
  //   */
  //  static Topology twoPeerGraph() {
  //
  //    // Some edges must be added _after_ the topology starts...
  //    final Topology topology = new Topology(new Topology.PostConstructListener() {
  //      @Override
  //      protected void doAfterGraphStartup(Topology g) {
  //        // Edges
  //        // Add all of alice's accounts as an edge between Alice and each account.
  //        accountsOfAlice(g).stream()
  //          .forEach(accountSettings -> g.addEdge(new ConnectorAccountEdge(ALICE_AT_CONNIE.getValue(), accountSettings)));
  //
  //        // Add all of bob's accounts as an edge between Bob and each account.
  //        accountsOfBob(g).stream()
  //          .forEach(accountSettings -> g.addEdge(new ConnectorAccountEdge(BOB_AT_CONNIE.getValue(), accountSettings)));
  //      }
  //
  //      //((ServerNode) topology.getNode(ALICE_AT_CONNIE.getValue())).getServer().setProperty(KEY_REMOTE_PEER_SCHEME, "ws");
  //      //((ServerNode) topology.getNode(ALICE_AT_CONNIE.getValue())).getServer().setProperty(KEY_REMOTE_PEER_HOSTNAME, "localhost");
  //      //((ServerNode) topology.getNode(ALICE_AT_CONNIE.getValue())).getServer().setProperty("server.port", "9000");
  //      //((ServerNode) topology.getNode(ALICE_AT_CONNIE.getValue())).getServer().setProperty(ILP_SERVER_PORT, "9000");
  //
  //      //((ServerNode) topology.getNode(BOB_AT_CONNIE.getValue())).getServer().setProperty(KEY_REMOTE_PEER_SCHEME, "ws");
  //      //((ServerNode) topology.getNode(BOB_AT_CONNIE.getValue())).getServer().setProperty(KEY_REMOTE_PEER_HOSTNAME, "localhost");
  //      //((ServerNode) topology.getNode(BOB_AT_CONNIE.getValue())).getServer().setProperty(ILP_SERVER_PORT, "9001");
  //
  //    });
  //
  //
  //    ///////////////////
  //    // Configure Alice
  //    topology.addNode(ALICE_AT_CONNIE.getValue(), new IlpConnectorNode(new ConnectorServer(defaultConnectorSettings(ALICE_AT_CONNIE))));
  //    // This must be set in this fashion in order for the Websocket Server to enable...
  //    toServerNode(topology, ALICE_AT_CONNIE).setProperty(ConnectorSettings.PROPERTY_NAME__WEBSOCKETS_ENABLED, "true");
  //
  //    ///////////////////
  //    // Configure Bob
  //    topology.addNode(BOB_AT_CONNIE.getValue(), new IlpConnectorNode(new ConnectorServer(defaultConnectorSettings(BOB_AT_CONNIE))));
  //
  //    LOGGER.info("\n" +
  //      "\nBTP 2-PEER ARCHITECTURE TEST" +
  //      "\n" +
  //      "┌──────────────┐           ┌──────────────┐\n" +
  //      "│              │           │              │\n" +
  //      "│  test.alice  │◁──BTP-───▷│  test.bob    │\n" +
  //      "│              │           │              │\n" +
  //      "└──────────────┘           └──────────────┘\n");
  //    return topology;
  //  }
  //
  //  /**
  //   * In this Architecture, Alice accounts with Bob.
  //   */
  //  private static List<AccountSettings> accountsOfAlice(final Topology topology) {
  //    Objects.requireNonNull(topology);
  //
  //    final Map<String, Object> customSettings = Maps.newConcurrentMap();
  //    customSettings.put("foo", "bar");
  //    customSettings.put(KEY_SECRET, "shh");
  //
  //    final PluginSettings pluginSettings = ImmutablePluginSettings.builder()
  //      // Alice is the BTP Server!
  //      .pluginType(ServerWebsocketBtpPlugin.PLUGIN_TYPE)
  //      .accountAddress(BOB_AT_CONNIE)
  //      .localNodeAddress(ALICE_AT_CONNIE)
  //      .customSettings(customSettings)
  //      .build();
  //
  //    // Peer with Bob (Bob is a CHILD of Alice's)
  //    final AccountSettings accountSettings = accountSettings(BOB_AT_CONNIE, USD, AccountSettings.IlpRelationship.CHILD, pluginSettings);
  //    return Lists.newArrayList(accountSettings);
  //  }
  //
  //  private static ConnectorSettings defaultConnectorSettings(final InterledgerAddress interledgerAddress) {
  //    return ImmutableConnectorSettings.builder()
  //      .ilpAddress(interledgerAddress)
  //      //.secret("secret")
  //      .build();
  //  }
  //
  //  /**
  //   * In this Architecture, Alice accounts with Bob.
  //   */
  //  private static List<AccountSettings> accountsOfBob(final Topology topology) {
  //    Objects.requireNonNull(topology);
  //
  //    // Because Bob is configured as a BTPClient, we need to addAccount the following fields to the CustomProperties section
  //    // in order for the plugin to be constructed properly.
  //    final Map<String, Object> customSettings = Maps.newConcurrentMap();
  //    customSettings.put("foo", "bar");
  //    customSettings.put(KEY_SECRET, "shh");
  //    customSettings.put(KEY_REMOTE_PEER_SCHEME, topology.getNodeAsServer(ALICE_AT_CONNIE.getValue()).getScheme());
  //    customSettings.put(KEY_REMOTE_PEER_HOSTNAME, topology.getNodeAsServer(ALICE_AT_CONNIE.getValue()).getHost());
  //    customSettings.put(KEY_REMOTE_PEER_PORT, topology.getNodeAsServer(ALICE_AT_CONNIE.getValue()).getPort());
  //
  //    final PluginSettings pluginSettings = ImmutablePluginSettings.builder()
  //      // Bob is the BTP Client!
  //      .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
  //      .accountAddress(ALICE_AT_CONNIE)
  //      .localNodeAddress(BOB_AT_CONNIE)
  //      .customSettings(customSettings)
  //      .build();
  //
  //    // Peer with Alice (Alice is a PARENT of Bob's)
  //    final AccountSettings aliceAccountSettings = accountSettings(ALICE_AT_CONNIE, USD, AccountSettings.IlpRelationship.PARENT, pluginSettings);
  //    return Lists.newArrayList(aliceAccountSettings);
  //  }
  //
  //  private static AccountSettings accountSettings(
  //    final InterledgerAddress accountIlpAddress, final String accountAssetCode,
  //    final AccountSettings.IlpRelationship ilpRelationship, final PluginSettings pluginSettings
  //  ) {
  //    Objects.requireNonNull(accountIlpAddress);
  //    Objects.requireNonNull(accountAssetCode);
  //    Objects.requireNonNull(pluginSettings);
  //
  //    return ImmutableAccountSettings.builder()
  //      .interledgerAddress(accountIlpAddress)
  //      .assetCode(accountAssetCode)
  //      .relationship(ilpRelationship)
  //      .pluginSettings(pluginSettings)
  //      .build();
  //  }
  //
  //  private static Server toServerNode(final Topology topology, final InterledgerAddress nodeName) {
  //    return ((ServerNode) topology.getNode(nodeName.getValue())).getServer();
  //  }
  //
  //  private static IlpConnectorNode toConnectorNode(final Topology topology, final InterledgerAddress nodeName) {
  //    return ((IlpConnectorNode) topology.getNode(nodeName.getValue()));
  //  }

}
