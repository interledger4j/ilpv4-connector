package org.interledger.ilpv4.connector.it.topologies.blast;

import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.IlpHttpController;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ModifiableConnectorSettings;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ModifiableAccountSettings;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.interledger.ilpv4.connector.it.topology.nodes.ConnectorServerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * <p>A very simple topology that simulates a single ILP-over-HTTP (BLAST) connection between two Connectors where
 * one Connector (`alice`) is the <tt>Parent</tt>, and the other Connector (`bob`) is the child.</p>
 *
 * <p>Nodes in this topology are connected as follows:</p>
 *
 * <pre>
 * ┌──────────────┐                     ┌──────────────┐
 * │              ◁───────HTTP/2────────┤              │
 * │              │                     │              │
 * │  CONNECTOR   │                     │  CONNECTOR   │
 * │  test.alice  │                     │   test.alice.│
 * │              │                     │    bob       │
 * │              ├──────HTTP/2─────────▷              │
 * └──────────────┘                     └──────────────┘
 * </pre>
 */
public class TwoConnectorParentChildBlastTopology {

  public static final String XRP = "XRP";

  public static final String ALICE = "alice";
  public static final String BOB = "bob";

  public static final int ALICE_PORT = 8080;
  public static final int BOB_PORT = 8081;

  public static final InterledgerAddress ALICE_ADDRESS = InterledgerAddress.of("test." + ALICE);
  public static final InterledgerAddress BOB_ADDRESS = ALICE_ADDRESS.with(BOB);

  public static final HttpUrl ALICE_TOKEN_ISSUER = HttpUrl.parse("https://" + ALICE + ".example.com");
  public static final HttpUrl BOB_TOKEN_ISSUER = HttpUrl.parse("https://" + BOB + ".example.com");

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorParentChildBlastTopology.class);

  static {
    // This is set to 0 so that the "port" value is used instead...
    System.setProperty("server.port", "0");
    System.setProperty("spring.jmx.enabled", "false");
    System.setProperty("spring.application.admin.enabled", "false");
  }

  /**
   * In this topology, each Connector starts-up with an Account for the other connector.
   *
   * @return
   */
  public static Topology init() {

    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
    final Topology topology = new Topology(new Topology.PostConstructListener() {
      @Override
      protected void doAfterTopologyStartup(Topology g) {

      }
    });

    ///////////////////
    // Alice Connector Node
    ///////////////////
    {
      final ConnectorServer aliceServer = new ConnectorServer(constructConnectorSettingsForAlice());
      aliceServer.setPort(ALICE_PORT);
      topology.addNode(ALICE_ADDRESS, new ConnectorServerNode(ALICE, aliceServer));
    }

    ///////////////////
    // Bob Connector Node
    ///////////////////

    {
      final ConnectorServer bobServer = new ConnectorServer(constructConnectorSettingsForBob());
      bobServer.setPort(BOB_PORT);
      topology.addNode(BOB_ADDRESS, new ConnectorServerNode(BOB, bobServer));
    }

    LOGGER.info("\n" +
      "\nSTARTING BLAST TOPOLOGY\n" +
      "┌──────────────┐                     ┌──────────────┐\n" +
      "│              ◁───────HTTP/2────────┤              │\n" +
      "│              │                     │              │\n" +
      "│  CONNECTOR   │                     │  CONNECTOR   │\n" +
      "│  test.alice  │                     │   test.alice.│\n" +
      "│              │                     │    bob       │\n" +
      "│              ├──────HTTP/2─────────▷              │\n" +
      "└──────────────┘                     └──────────────┘\n"
    );
    return topology;
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Alice</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForAlice() {

    final AccountSettings accountSettings = AccountSettings.builder()
      .id(AccountId.of(BOB))
      .description("Blast account for Bob @ Alice")
      .isPreconfigured(true)
      .relationship(AccountRelationship.PEER)
      .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .linkType(BlastLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_ID, BOB)
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)

      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_ID, ALICE)
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_AUTH_TYPE, "JWT")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, "PT2M")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
      .putCustomSettings(
        BlastLinkSettings.BLAST_OUTGOING_URL,
        "http://localhost:" + BOB_PORT + IlpHttpController.ILP_PATH
      )

      .build();

    final ConnectorSettings connectorSettings = ImmutableConnectorSettings.builder()
      .operatorAddress(ALICE_ADDRESS)
      .jwtTokenIssuer(ALICE_TOKEN_ISSUER)
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isBlastEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerConfigEnabled(true)
        .isPeerRoutingEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        .routingSecret("DocIHaveToTellYouSomethingAboutY")
        .build()
      )
      .addAccountSettings(
        // Must be modifiable so that we can update these settings after the Topology starts (to assign ports).
        // This type of non-immutability is only used for tests like this one, and should not be used in production.
        ModifiableAccountSettings.create().from(accountSettings)
      )
      .build();

    // Must be modifiable to support IL-DCP
    return ModifiableConnectorSettings.create().from(connectorSettings);
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Bob</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForBob() {

    final AccountSettings accountSettings = AccountSettings.builder()
      .id(AccountId.of(ALICE))
      .description("Blast account for Alice @ Bob")
      .isPreconfigured(true)
      .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .relationship(AccountRelationship.PARENT)
      .linkType(BlastLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_ID, ALICE)
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)

      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_ID, BOB)
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_AUTH_TYPE, "JWT")
      // This is used by the BLAST Sender in order to assemble the JWT properly...Bob must sign the token and make
      // himself the issuer.
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER) // Replaced by
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, "PT2M")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_URL,
        "http://localhost:" + ALICE_PORT + IlpHttpController.ILP_PATH)

      .build();

    final ConnectorSettings connectorSettings = ImmutableConnectorSettings.builder()
      .jwtTokenIssuer(BOB_TOKEN_ISSUER)
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isBlastEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerConfigEnabled(false)
        .isPeerRoutingEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        .routingSecret("DocIHaveToTellYouSomethingAboutY")
        .build()
      )
      .addAccountSettings(
        // Must be modifiable so that we can update these settings after the Topology starts (to assign ports).
        // This type of non-immutability is only used for tests like this one, and should not be used in production.
        ModifiableAccountSettings.create().from(accountSettings)
      )

      .build();

    // Must be modifiable to support IL-DCP
    return ModifiableConnectorSettings.create().from(connectorSettings);
  }

}
