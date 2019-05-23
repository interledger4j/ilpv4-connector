package org.interledger.ilpv4.connector.it.topologies.btp;

/**
 * <p>A very simple topology that simulates a single BTP connection between a Sender (Alice) and a Receiver (Connie)
 * using BTP as the bilateral connection between the two.</p>
 *
 * <p>In this topology, Alice has a USD account with Connie (meaning Alice and Connie can owe each other USD). Alice
 * can pay Connie directly using a simple BTP client.</p>
 *
 * <p>Nodes in this topology are connected as follows:</p>
 *
 * <pre>
 *
 *           ┌───────────Setup──────────────┐
 *           │                              │
 *           │                              ▼
 *           │                      ┌──────────────┐
 *           │                      │              │
 * ┌───────────────────┐            │              │
 * │  CLIENT PLUGIN    │            │  CONNECTOR   │
 * │ test.connie.alice │───USD─────▷│ test.connie  │
 * └───────────────────┘            │              │
 *                                  │              │
 *                                  └──────────────┘
 * </pre>
 *
 * @deprecated Remove once Blast ITs are completed.
 */
public class SingleConnectorSingleAccountBtpTopology {

//  public static final String USD = "USD";
  //
  //  public static final InterledgerAddress ALICE = InterledgerAddress.of("test.alice");
  //  public static final InterledgerAddress CONNIE = InterledgerAddress.of("test.connie");
  //
  //  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorSingleAccountBtpTopology.class);
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
  //    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
  //    final Topology topology = new Topology(new Topology.PostConstructListener() {
  //      @Override
  //      protected void doAfterTopologyStartup(Topology g) {
  //
  //        final ConnectorServerNode connieServerNode = g.getNode(CONNIE.getValue(), ConnectorServerNode.class);
  //        final int conniePort = connieServerNode.getPort();
  //
  //        ////////////////////
  //        // TODO remove this once a proper administrative system + listener is added to the Connector. For example, an
  //        //  adminstrative function called "reload" should exist, which should trigger an event to all sub-services to
  //        //  reload themselves. The primary use-case is this IT where the RoutingSettings first get loaded from the
  //        //  .yml file, and then replaced in ConnectorServer. In ConnectorServer, we would want to call `reload` so
  //        //  that all sub-services re-initialized themselves with the new ConnectorSettings. This would also be useful
  //        //  if ConnectorSettings ever change during runtime. For now, however, this is limited to the ExternalRoutingService,
  //        // so we just manually trigger this here.
  //        //  @depracated
  //        connieServerNode.getILPv4Connector().getExternalRoutingService().start();
  //
  //        // Connect Alice to Connie.
  //        final BtpClientPluginNode pluginNode = new BtpClientPluginNode(constructClientConnection(conniePort, ALICE));
  //        g.addNode(ALICE, pluginNode);
  //        pluginNode.getContentObject().connect().join();
  //      }
  //    });
  //
  //    ///////////////////
  //    // Connie Node
  //    ///////////////////
  //    {
  //      topology.addNode(CONNIE, new ConnectorServerNode(new ConnectorServer(constructConnectorSettingsForConnie())));
  //    }
  //
  //    LOGGER.info("\n" +
  //      "\nSTARTING SINGLE-ACCOUNT 1-CONNECTOR TOPOLOGY" +
  //      "                                                 \n" +
  //      "          ┌───────────Setup──────────────┐       \n" +
  //      "          │                              │       \n" +
  //      "          │                              ▼       \n" +
  //      "          │                      ┌──────────────┐\n" +
  //      "          │                      │              │\n" +
  //      "┌───────────────────┐            │              │\n" +
  //      "│      SENDER       │            │  CONNECTOR   │\n" +
  //      "│ test.connie.alice │───USD─────▷│ test.connie  │\n" +
  //      "└───────────────────┘            │              │\n" +
  //      "                                 │              │\n" +
  //      "                                 └──────────────┘\n"
  //    );
  //    return topology;
  //  }
  //
  //  /**
  //   * Construct an {@link BtpClientPlugin} so Alice can connect to Connie.
  //   *
  //   * @param conniePort      The port that Connie is operating at.
  //   * @param operatorAddress The address of the operator of the account.
  //   */
  //  private static BtpClientPlugin constructClientConnection(
  //    final int conniePort, final InterledgerAddress operatorAddress
  //  ) {
  //    Objects.requireNonNull(operatorAddress);
  //
  //    final BtpClientPluginSettings pluginSettings = BtpClientPluginSettings.builder()
  //      .operatorAddress(ALICE)
  //      .authUsername("alice")
  //      .secret("shh")
  //      .remotePeerScheme("ws")
  //      .remotePeerHostname("localhost")
  //      .remotePeerPort(conniePort)
  //      .sendMoneyWaitTime(Duration.of(30, ChronoUnit.SECONDS))
  //      .build();
  //
  //    final BtpSubProtocolHandlerRegistry registry = new BtpSubProtocolHandlerRegistry(
  //      new ClientAuthBtpSubprotocolHandler()
  //    );
  //    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter =
  //      new BinaryMessageToBtpPacketConverter(BtpCodecContextFactory.oer());
  //    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter =
  //      new BtpPacketToBinaryMessageConverter(BtpCodecContextFactory.oer());
  //
  //    final BtpClientPlugin clientPlugin = new BtpClientPlugin(
  //      pluginSettings,
  //      InterledgerCodecContextFactory.oer(),
  //      binaryMessageToBtpPacketConverter,
  //      btpPacketToBinaryMessageConverter,
  //      registry,
  //      new StandardWebSocketClient()
  //    );
  //
  //    return clientPlugin;
  //  }
  //
  //  /**
  //   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Connie</tt>.
  //   */
  //  public static ConnectorSettings constructConnectorSettingsForConnie() {
  //
  //    // Used for default BTP account values...
  //    final AccountProviderSettings btpServerAccountSettings =
  //      AccountProviderSettings.builder()
  //        .id(AccountProviderId.of(BtpServerPlugin.PLUGIN_TYPE.value()))
  //        .description("BTP Child accounts")
  //        .relationship(AccountRelationship.CHILD)
  //        .pluginType(BtpServerPlugin.PLUGIN_TYPE)
  //        .assetScale(9)
  //        .assetCode(USD)
  //        .build();
  //
  //    return ImmutableConnectorSettings.builder()
  //      .operatorAddress(CONNIE)
  //      .websocketServerEnabled(true)
  //      .enabledProtocols(EnabledProtocolSettings.builder()
  //        .isPingProtocolEnabled(true)
  //        .isEchoProtocolEnabled(true)
  //        .build())
  //      .globalPrefix(InterledgerAddressPrefix.TEST)
  //      .globalRoutingSettings(GlobalRoutingSettings.builder()
  //        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
  //        // plaintext value of `shh`, encrypted.
  //        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
  //        .build()
  //      )
  //      .addAccountProviderSettings(btpServerAccountSettings)
  //      .build();
  //  }

}
