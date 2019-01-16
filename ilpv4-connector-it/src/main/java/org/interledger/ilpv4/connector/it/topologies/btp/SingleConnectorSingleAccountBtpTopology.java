package org.interledger.ilpv4.connector.it.topologies.btp;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import com.sappenin.interledger.ilpv4.connector.settings.AccountRelationship;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import org.interledger.btp.asn.framework.BtpCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.ilpv4.connector.it.topologies.TopologyUtils;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.interledger.ilpv4.connector.it.topology.nodes.btp.BtpServerNode;
import org.interledger.ilpv4.connector.it.topology.nodes.btp.BtpSingleAccountClientNode;
import org.interledger.plugin.lpiv2.btp2.spring.BtpClientPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.BtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.BtpServerPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.ClientAuthBtpSubprotocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Objects;

/**
 * <p>A very simple topology that simulates a single BTP connection between a Sender (Alice) and a Receiver (Connie)
 * using
 * BTP as the bilateral connection between the two.</p>
 *
 * <p>In this topology, Alice has a USD account with Connie (meaning Alice and Connie can owe each other USD). Alice
 * can
 * pay Connie directly using a simple BTP client.</p>
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
 * │  CLIENT PLUGIN_MODE    │            │  CONNECTOR_MODE   │
 * │ test.connie.alice │───USD─────▷│ test.connie  │
 * └───────────────────┘            │              │
 *                                  │              │
 *                                  └──────────────┘
 * </pre>
 */
public class SingleConnectorSingleAccountBtpTopology {

  public static final String USD = "USD";

  public static final InterledgerAddress ALICE = InterledgerAddress.of("test.alice");
  public static final InterledgerAddress CONNIE = InterledgerAddress.of("test.connie");
  //public static final InterledgerAddress ALICE_AT_CONNIE = CONNIE.with("alice");

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleConnectorSingleAccountBtpTopology.class);

  static {
    // This is set to 0 so that the "port" value is used instead...
    System.setProperty("server.port", "0");
    System.setProperty("spring.jmx.enabled", "false");
    System.setProperty("spring.application.admin.enabled", "false");
  }

  public static Topology init() {

    // Some configuration must be done _after_ the topology starts...
    final Topology topology = new Topology(new Topology.PostConstructListener() {
      @Override
      protected void doAfterTopologyStartup(Topology g) {

        final int conniePort = g.getNodeAsServer(CONNIE).getPort();

        // Connect Alice to Connie.
        g.addNode(ALICE,
          new BtpSingleAccountClientNode(constructClientConnection(conniePort, ALICE)));
      }
    });

    ///////////////////
    // Connie Node
    ///////////////////
    {
      topology
        .addNode(CONNIE, new BtpServerNode(new ConnectorServer(constructConnectorSettingsForConnie())));
      // This must be set before startup in order for the Websocket Server to enable...
      TopologyUtils.toServerNode(topology, CONNIE).setProperty(ConnectorProperties.WEBSOCKET_SERVER_ENABLED, "true");
    }

    LOGGER.info("\n" +
      "\nSTARTING SINGLE-ACCOUNT 1-CONNECTOR_MODE TOPOLOGY" +
      "                                                 \n" +
      "          ┌───────────Setup──────────────┐       \n" +
      "          │                              │       \n" +
      "          │                              ▼       \n" +
      "          │                      ┌──────────────┐\n" +
      "          │                      │              │\n" +
      "┌───────────────────┐            │              │\n" +
      "│      SENDER       │            │  CONNECTOR   │\n" +
      "│ test.connie.alice │───USD─────▷│ test.connie  │\n" +
      "└───────────────────┘            │              │\n" +
      "                                 │              │\n" +
      "                                 └──────────────┘\n"
    );
    return topology;
  }

  /**
   * Construct an {@link BtpClientPlugin} so Alice can connect to Connie.
   *
   * @param conniePort      The port that Connie is operating at.
   * @param operatorAddress The address of the operator of the account.
   */
  private static BtpClientPlugin constructClientConnection(
    final int conniePort, final InterledgerAddress operatorAddress
  ) {
    Objects.requireNonNull(operatorAddress);

    final BtpClientPluginSettings pluginSettings = BtpClientPluginSettings.builder()
      .operatorAddress(ALICE)
      .authUsername("alice")
      .secret("shh")
      .remotePeerScheme("ws")
      .remotePeerHostname("localhost")
      .remotePeerPort(conniePort)
      .build();

    final BtpSubProtocolHandlerRegistry registry = new BtpSubProtocolHandlerRegistry(
      new ClientAuthBtpSubprotocolHandler()
    );
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter =
      new BinaryMessageToBtpPacketConverter(BtpCodecContextFactory.oer());
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter =
      new BtpPacketToBinaryMessageConverter(BtpCodecContextFactory.oer());

    final BtpClientPlugin clientPlugin = new BtpClientPlugin(
      pluginSettings,
      InterledgerCodecContextFactory.oer(),
      binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter,
      registry,
      new StandardWebSocketClient()
    );

    return clientPlugin;
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Connie</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForConnie() {

    // Used for default-account values...
    final AccountSettings btpServerAccountSettings =
      AccountSettings.builder()
        .id(AccountId.of("btp-accounts"))
        .description("A single-account BTP connection this connector from Alice.")
        .relationship(AccountRelationship.CHILD)
        .pluginType(BtpServerPlugin.PLUGIN_TYPE)
        .assetScale(9)
        .assetCode(USD)
        .build();

    return ImmutableConnectorSettings.builder()
      .operatorAddress(CONNIE)
      .websocketServerEnabled(true)
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .addAccountSettings(btpServerAccountSettings)
      .build();
  }

}
