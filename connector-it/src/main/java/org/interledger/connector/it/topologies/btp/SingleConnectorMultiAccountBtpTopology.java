package org.interledger.connector.it.topologies.btp;

/**
 * <p>A very simple topology that simulates BTP connections between a Sender (Alice) and a Receiver (Bob) who both
 * speak BTP to a Connector (Connie).
 *
 * <p>In this topology, Alice has a USD account with Connie (meaning Alice and Connie can owe each other USD).
 * Likewise, Connie and Bob have an EUR account (meaning Bob and Connie can owe each other EUR). Alice can pay Bob by
 * utilizing liquidity held by Connie.</p>
 *
 * <p>Nodes in this topology are connected as follows:</p>
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
 *
 * @deprecated Remove once Blast ITs are completed.
 */
@Deprecated
public class SingleConnectorMultiAccountBtpTopology {

  //  public static final String USD = "USD";
  //  public static final String EUR = "EUR";
  //
  //  public static final InterledgerAddress ALICE_CONNECTOR_ADDRESS = InterledgerAddress.of("test.alice");
  //  public static final InterledgerAddress BOB_CONNECTOR_ADDRESS = InterledgerAddress.of("test.bob");
  //  public static final InterledgerAddress CONNIE = InterledgerAddress.of("test.connie");
  //  public static final InterledgerAddress ALICE_AT_CONNIE = CONNIE.with("alice");
  //  public static final InterledgerAddress BOB_AT_CONNIE = CONNIE.with("bob");
  //
  //  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorMultiAccountBtpTopology.class);
  //
  //  static {
  //    // This is set to 0 so that the "port" value is used instead...
  //    System.setProperty("server.port", "0");
  //    System.setProperty("spring.jmx.enabled", "false");
  //    System.setProperty("spring.application.admin.enabled", "false");
  //  }
  //
  //  public static Topology init() {
  //
  //    // Some edges must be added _after_ the topology starts...
  //    final Topology topology = new Topology(new Topology.PostConstructListener() {
  //      @Override
  //      protected void doAfterGraphStartup(Topology g) {
  //
  //        // Connect Alice to Connie.
  //        g.addNode(ALICE_CONNECTOR_ADDRESS, new BtpSingleAccountClientNode(constructBtpWebsocketClient(g, ALICE_CONNECTOR_ADDRESS, ALICE_AT_CONNIE)));
  //
  //
  //        //        g.addEdge(
  //        //          new ClientWebsocketBtpPluginEdge(ALICE_AT_CONNIE, connieAccountInAlice(g))
  //        //        );
  //
  //        // Connect Bob to Connie.
  //        g.addNode(BOB_CONNECTOR_ADDRESS, new BtpSingleAccountClientNode(constructBtpWebsocketClient(g, BOB_CONNECTOR_ADDRESS, BOB_AT_CONNIE)));
  //
  //        //        g.addEdge(
  //        //          new ClientWebsocketBtpPluginEdge(BOB_AT_CONNIE, connieAccountInBob(g))
  //        //        );
  //
  //
  //      }
  //    });
  //
  //    ///////////////////
  //    // Configure Alice
  //    ///////////////////
  //    {
  //
  //    }
  //    ///////////////////
  //    // Configure Connie
  //    ///////////////////
  //    {
  //      topology.addNode(CONNIE, new ConnectorServerNode(new ConnectorServer(constructConnectorSettingsForConnie())));
  //      // This must be set before startup in order for the Websocket Server to enable...
  //      //toServerNode(topology, CONNIE).setProperty(ConnectorSettings.PROPERTY_NAME__WEBSOCKETS_ENABLED, "true");
  //    }
  //
  //    ///////////////////
  //    // Configure Bob
  //    ///////////////////
  //    {
  //      //topology.addNode(BOB_CONNECTOR_ADDRESS, new BtpSingleAccountClientNode(constructSingleAccountConnection(BOB_CONNECTOR_ADDRESS, BOB_AT_CONNIE)));
  //    }
  //
  //    LOGGER.info("\n" +
  //      "\nSTARTING 1-CONNECTOR TOPOLOGY" +
  //      "\n" +
  //      "                                                                                \n" +
  //      "          ┌────────────────────────────Setup──────────────────────────┐         \n" +
  //      "          │                      ┌──────────────┐                     │         \n" +
  //      "          │                      │              │                     ▼         \n" +
  //      "┌───────────────────┐            │              │           ┌──────────────────┐\n" +
  //      "│      SENDER       │            │  CONNECTOR   │           │     RECEIVER     │\n" +
  //      "│ test.connie.alice │───USD─────▷│ test.connie  │────USD───▶│ test.connie.bob  │\n" +
  //      "└───────────────────┘            │              │           └──────────────────┘\n" +
  //      "                                 │              │                               \n" +
  //      "                                 └──────────────┘                               \n"
  //    );
  //    return topology;
  //  }
  //
  //  /**
  //   * Construct an {@link SingleAccountBtpClientConnection} object to connect Alice to Connie.
  //   */
  //  private static ClientWebsocketBtpPlugin constructBtpWebsocketClient(
  //    final Topology topology,
  //    final InterledgerAddress operatorAddress, final InterledgerAddress accountAddress
  //  ) {
  //    Objects.requireNonNull(operatorAddress);
  //    Objects.requireNonNull(accountAddress);
  //
  //    final CodecContext ilpCodecContext = InterledgerCodecContextFactory.oer();
  //    final CodecContext btpCodecContext = BtpCodecContextFactory.oer();
  //
  //    final BtpSubProtocolHandlerRegistry registry = new BtpSubProtocolHandlerRegistry(
  //      new ClientAuthBtpSubprotocolHandler()
  //    );
  //
  //    final int conniePort = topology.getNodeAsServer(CONNIE).getPort();
  //
  //    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter =
  //      new BinaryMessageToBtpPacketConverter(btpCodecContext);
  //    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter =
  //      new BtpPacketToBinaryMessageConverter(btpCodecContext);
  //
  //    final ClientBtpWebsocketMux websocketMux = new ClientBtpWebsocketMux(
  //      binaryMessageToBtpPacketConverter,
  //      btpPacketToBinaryMessageConverter,
  //      registry,
  //      ALICE_AT_CONNIE,
  //      Optional.empty(), // Single-account clients don't use `auth_username`
  //      "shh",
  //      "ws",
  //      "localhost",
  //      conniePort + "",
  //      new StandardWebSocketClient()
  //    );
  //    //   final SingleAccountBtpClientConnection connection = new SingleAccountBtpClientConnection(
  //    //      operatorAddress,
  //    //      accountAddress,
  //    //      websocketMux
  //    //    );
  //
  //    final BtpPluginSettings pluginSettings = ImmutableBtpPluginSettings.builder()
  //      .secret("shh")
  //      .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
  //      .accountAddress(accountAddress)
  //      .localNodeAddress(operatorAddress)
  //      .build();
  //    final ClientWebsocketBtpPlugin plugin = new ClientWebsocketBtpPlugin(
  //      pluginSettings,
  //      //ilpCodecContext, btpCodecContext, registry, binaryMessageToBtpPacketConverter,
  //      // btpPacketToBinaryMessageConverter,
  //      websocketMux
  //    );
  //    return plugin;
  //  }
  //
  //  //  /**
  //  //   * Construct an {@link AccountSettings} object to represent Bob's account inside of Connie.
  //  //   */
  //  //  private static AccountSettings accountInConnie(final Topology topology, final InterledgerAddress clientAddress) {
  //  //    Objects.requireNonNull(topology);
  //  //
  //  //    // Because the client/child is configured as a BTPClient, we need to addAccount the following fields to the
  //  //    // CustomProperties section in order for the plugin to be constructed properly.
  //  //    final Map<String, Object> customSettings = Maps.newConcurrentMap();
  //  //    customSettings.put("foo", "bar");
  //  //    customSettings.put(KEY_SECRET, "shh");
  //  //    customSettings.put(KEY_USER_NAME, clientAddress.getValue());
  //  //
  //  //    final PluginSettings pluginSettings = ImmutablePluginSettings.builder()
  //  //      // Connie is the BTP Server!
  //  //      .pluginType(ServerWebsocketBtpPlugin.PLUGIN_TYPE)
  //  //      .accountAddress(clientAddress)
  //  //      .localNodeAddress(CONNIE)
  //  //      .customSettings(customSettings)
  //  //      .build();
  //  //
  //  //    return ImmutableAccountSettings.builder()
  //  //      .assetCode(USD)
  //  //      .relationship(PARENT) // Connie is the PARENT of the client/child address.
  //  //      .pluginSettings(pluginSettings)
  //  //      .build();
  //  //  }
  //
  //  /**
  //   * Construct an {@link AccountSettings} object to represent Alice's account inside of Connie.
  //   */
  //  private static LoopbackPlugin connieAccountInAlice(final Topology topology) {
  //    Objects.requireNonNull(topology);
  //
  //    // TODO: For the plugin layer, we should not use a loopback plugin. We should use a no-op MoneyHandler, but send
  //    //  real-data packets.
  //    return new LoopbackPlugin(ALICE_AT_CONNIE);
  //    //return constructBtpWsClientPlugin(topology, ALICE_AT_CONNIE, CONNIE);
  //  }
  //
  //  /**
  //   * Construct an {@link AccountSettings} object to represent Alice's account inside of Connie.
  //   */
  //  private static LoopbackPlugin connieAccountInBob(final Topology topology) {
  //    Objects.requireNonNull(topology);
  //
  //    // TODO: For the plugin layer, we should not use a loopback plugin. We should use a no-op MoneyHandler, but send
  //    //  real-data packets.
  //    return new LoopbackPlugin(BOB_AT_CONNIE);
  //    //return constructBtpWsClientPlugin(topology, BOB_AT_CONNIE, CONNIE);
  //  }
  //
  //  //  /**
  //  //   * Construct an {@link AccountSettings} object to represent Alice's account inside of Connie.
  //  //   *
  //  //   * @param topology             The {@link Topology} that contains nodes.
  //  //   * @param accountAddress    The {@link InterledgerAddress} that identifies the bilateral account from the perspective
  //  //   *                          of the client.
  //  //   * @param remoteNodeAddress The {@link InterledgerAddress} of the remote peer, from the perspective of the topology.
  //  //   */
  //  //  private static ClientWebsocketBtpPlugin constructBtpWsClientPlugin(
  //  //    final Topology topology,
  //  //    final InterledgerAddress accountAddress,
  //  //    final InterledgerAddress remoteNodeAddress
  //  //  ) {
  //  //    Objects.requireNonNull(topology);
  //  //
  //  //    // Because Alice is configured as a BTPClient, we need to addAccount the following fields to the CustomProperties section
  //  //    // in order for the Connection to be constructed properly.
  //  //    final Map<String, Object> customSettings = Maps.newConcurrentMap();
  //  //    customSettings.put("foo", "bar");
  //  //    customSettings.put(KEY_SECRET, "shh");
  //  //    customSettings.put(KEY_USER_NAME, accountAddress.getValue());
  //  //    customSettings.put(KEY_REMOTE_PEER_SCHEME, topology.getNodeAsServer(remoteNodeAddress).getScheme());
  //  //    customSettings.put(KEY_REMOTE_PEER_HOSTNAME, topology.getNodeAsServer(remoteNodeAddress).getHost());
  //  //    customSettings.put(KEY_REMOTE_PEER_PORT, topology.getNodeAsServer(remoteNodeAddress).getPort());
  //  //
  //  //    final BtpClientPluginSettings pluginSettings = BtpClientPluginSettings.builder(customSettings)
  //  //      .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
  //  //      .accountAddress(remoteNodeAddress)
  //  //      .localNodeAddress(accountAddress)
  //  //      .build();
  //  //
  //  //    final CodecContext ilpCodecContext = InterledgerCodecContextFactory.oer();
  //  //    final CodecContext btpCodecContext = BtpCodecContextFactory.oer();
  //  //
  //  //    final AuthBtpSubprotocolHandler authBtpSubprotocolHandler = new ServerAuthBtpSubprotocolHandler(
  //  //      new BtpAuthenticator() {
  //  //        @Override
  //  //        public boolean isValidAuthToken(final String incomingAuthToken) {
  //  //          Objects.requireNonNull(incomingAuthToken);
  //  //          return Optional.ofNullable(getAccount().customSettings().get("btpAuthToken"))
  //  //            .map(presentedAuthToken -> presentedAuthToken.equals(incomingAuthToken))
  //  //            .orElseThrow(() -> new RuntimeException("BTP `auth_token` must be specifed in Account Settings!"));
  //  //        }
  //  //
  //  //        @Override
  //  //        public InterledgerAddress getAccountAddress() {
  //  //          return BtpSingleAccountConnectorProfile.this.getAccountAddress();
  //  //        }
  //  //      },
  //  //      new BtpMultiAuthenticator.AlwaysAllowedBtpMultiAuthenticator(connectorSettings.operatorAddress())
  //  //    );
  //  //
  //  //    final AuthBtpSubprotocolHandler authBtpSubprotocolHandler = new ServerAuthBtpSubprotocolHandler();
  //  //    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry = new BtpSubProtocolHandlerRegistry(
  //  //      authBtpSubprotocolHandler
  //  //    );
  //  //    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter =
  //  //      new BinaryMessageToBtpPacketConverter(btpCodecContext);
  //  //    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter =
  //  //      new BtpPacketToBinaryMessageConverter(btpCodecContext);
  //  //
  //  //    final StandardWebSocketClient wsClient = new StandardWebSocketClient();
  //  //
  //  //    return new ClientWebsocketBtpPlugin(
  //  //      pluginSettings, ilpCodecContext, btpCodecContext, btpSubProtocolHandlerRegistry,
  //  //      binaryMessageToBtpPacketConverter, btpPacketToBinaryMessageConverter, wsClient
  //  //    );
  //  //  }
  //
  //  public static ConnectorSettings constructConnectorSettingsForConnie() {
  //    final MultiAccountConnectionSettings defaultConnectionSettings =
  //      ImmutableMultiAccountConnectionSettings.builder()
  //        .bilateralConnectionType(LoopbackConnection.CONNECTION_TYPE)
  //        .description("A multi-account BTP connection this connector.")
  //        .routeBroadcastSettings(ImmutableRouteBroadcastSettings.builder()
  //          .routingSecret("shh")
  //          //.routeExpiry(Duration.ofMinutes(1))
  //          //.routeCleanupInterval(Duration.ofMinutes(15))
  //          //.rout
  //          .build()
  //        )
  //        .defaultAccountSettings(ImmutableDefaultAccountSettings.builder()
  //          .relationship(AccountRelationship.PARENT)
  //          // PluginSettings
  //          .defaultPluginSettings(ImmutableDefaultPluginSettings.builder()
  //            .pluginType(LoopbackPlugin.PLUGIN_TYPE)
  //            .build()
  //          )
  //          .balanceSettings(ImmutableAccountBalanceSettings.builder().build())
  //          .build())
  //        .build();
  //
  //    return ImmutableConnectorSettings.builder()
  //      .nodeIlpAddress(CONNIE)
  //      .multiAccountConnections(Lists.newArrayList(defaultConnectionSettings))
  //      .build();
  //  }

}
