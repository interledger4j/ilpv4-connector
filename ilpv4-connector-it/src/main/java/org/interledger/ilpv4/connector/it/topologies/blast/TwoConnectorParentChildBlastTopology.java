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
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.IncomingLinkSettings;
import org.interledger.connector.link.blast.OutgoingLinkSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.interledger.ilpv4.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
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
  private static final String SHARED_SECRET = "12345678912345678912345678912345";
  private static final String PT2M = "PT2M";
  private static final String JWT = "JWT_HS_256";

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

        final ConnectorServerNode aliceServerNode = g.getNode(ALICE_ADDRESS.getValue(), ConnectorServerNode.class);
        final int alicePort = aliceServerNode.getPort();
        final ConnectorServerNode bobServerNode = g.getNode(BOB_ADDRESS.getValue(), ConnectorServerNode.class);
        final int bobPort = bobServerNode.getPort();

        // Add Bob's account on Alice...
        final AccountSettingsEntity bobAccountSettingsAtAlice = constructBobAccountSettingsOnAlice(alicePort);
        aliceServerNode.getILPv4Connector().getAccountSettingsRepository().save(bobAccountSettingsAtAlice);

        // Add Alice's account on Bob...
        final AccountSettingsEntity aliceAccountSettingsAtBob = constructAliceAccountSettingsOnBob(bobPort);
        bobServerNode.getILPv4Connector().getAccountSettingsRepository().save(aliceAccountSettingsAtBob);
        bobServerNode.getILPv4Connector().getAccountManager()
          .initializeParentAccountSettingsViaIlDcp(aliceAccountSettingsAtBob);

        // Try to connect the bob account...
        aliceServerNode.getILPv4Connector().getLinkManager().createLink(
          bobAccountSettingsAtAlice.getAccountId(),
          LinkSettings.builder()
            .linkType(bobAccountSettingsAtAlice.getLinkType())
            .customSettings(bobAccountSettingsAtAlice.getCustomSettings())
            .build()
        );

        // Try to connect the alice account...
        bobServerNode.getILPv4Connector().getLinkManager().createLink(
          aliceAccountSettingsAtBob.getAccountId(),
          LinkSettings.builder()
            .linkType(aliceAccountSettingsAtBob.getLinkType())
            .customSettings(aliceAccountSettingsAtBob.getCustomSettings())
            .build()
        );
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
      .build();

    // Must be modifiable to support IL-DCP
    return ModifiableConnectorSettings.create().from(connectorSettings);
  }

  /**
   * An AccountSettings object that represents Bob's account at Alice.
   *
   * @param bobPort The port that bob's server is running on.
   */
  private static AccountSettingsEntity constructBobAccountSettingsOnAlice(final int bobPort) {
    return new AccountSettingsEntity(
      AccountSettings.builder()
        .accountId(AccountId.of(BOB))
        .description("Blast account for Bob @ Alice")
        .accountRelationship(AccountRelationship.CHILD)
        .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
        .linkType(BlastLink.LINK_TYPE)
        .isConnectionInitiator(true)
        .assetScale(9)
        .assetCode(XRP)

        // Alice, the Connector Account operator, has an account that she manages, allowing Bob to connect. This
        // account has an id of `bob` (i.e., Bob's account with Alice). Thus, the incoming account-id for this link
        // will be `bob`. The token-issuer in this case will also be Bob, since he is generating the token for the
        // incoming request.

        // Incoming
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT, BOB)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, SHARED_SECRET)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, ALICE_TOKEN_ISSUER)

        // Outgoing
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, ALICE)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, SHARED_SECRET)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, "PT2M")
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, BOB_TOKEN_ISSUER)
        .putCustomSettings(
          OutgoingLinkSettings.BLAST_OUTGOING_URL, "http://localhost:" + bobPort + IlpHttpController.ILP_PATH
        )

        .build()
    );
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Bob</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForBob() {

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
      .build();

    // Must be modifiable to support IL-DCP
    return ModifiableConnectorSettings.create().from(connectorSettings);
  }

  public static AccountSettingsEntity constructAliceAccountSettingsOnBob(final int alicePort) {
    return new AccountSettingsEntity(
      AccountSettings.builder()
        .accountId(AccountId.of(ALICE))
        .description("Blast account for Alice @ Bob")
        .isConnectionInitiator(true)
        .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
        .accountRelationship(AccountRelationship.PARENT)
        .linkType(BlastLink.LINK_TYPE)
        .assetScale(9)
        .assetCode(XRP)

        // Bob, the Connector Account operator, has an account that he manages, allowing Alice to connect. This account
        // has an id of `alice` (i.e., Alice's account with Bob). Thus, the incoming account-id for this link will be
        // `alice`. The token-issuer in this case will also be Alice, since she is generating the token for the
        // incoming request.

        // Incoming
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT, ALICE)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, SHARED_SECRET)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, BOB_TOKEN_ISSUER)
        //        .putCustomSettings(
        //          IncomingLinkSettings., "http://localhost:" + alicePort + IlpHttpController.ILP_PATH
        //        )

        // Outgoing
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, BOB)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, "shh")
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, "PT2M")
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, ALICE_TOKEN_ISSUER)
        .putCustomSettings(
          OutgoingLinkSettings.BLAST_OUTGOING_URL, "http://localhost:" + alicePort + IlpHttpController.ILP_PATH
        )

        .build()
    );
  }

}
