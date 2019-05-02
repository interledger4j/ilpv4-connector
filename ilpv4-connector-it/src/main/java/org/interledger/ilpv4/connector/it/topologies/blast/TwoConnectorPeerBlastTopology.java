package org.interledger.ilpv4.connector.it.topologies.blast;

import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.IlpHttpController;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
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
 * <p>A very simple topology that simulates a single ILP-over-HTTP (BLAST) connection between two Connectors.</p>
 *
 * <p>Nodes in this topology are connected as follows:</p>
 *
 * <pre>
 * ┌──────────────┐                     ┌──────────────┐
 * │              ◁───────HTTP/2────────┤              │
 * │              │                     │              │
 * │  CONNECTOR   │                     │  CONNECTOR   │
 * │  test.alice  │                     │   test.bob   │
 * │              │                     │              │
 * │              ├──────HTTP/2─────────▷              │
 * └──────────────┘                     └──────────────┘
 * </pre>
 */
public class TwoConnectorPeerBlastTopology {

  public static final String XRP = "XRP";

  public static final String ALICE = "alice";
  public static final String BOB = "bob";

  public static final int ALICE_PORT = 8080;
  public static final int BOB_PORT = 8081;

  public static final InterledgerAddress ALICE_ADDRESS = InterledgerAddress.of("test." + ALICE);
  public static final InterledgerAddress BOB_ADDRESS = InterledgerAddress.of("test." + BOB);

  public static final HttpUrl ALICE_TOKEN_ISSUER = HttpUrl.parse("https://" + ALICE + ".example.com");
  public static final HttpUrl BOB_TOKEN_ISSUER = HttpUrl.parse("https://" + BOB + ".example.com");

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorPeerBlastTopology.class);

  static {
    // This is set to 0 so that the "port" value is used instead...
    System.setProperty("server.port", "0");
    System.setProperty("spring.jmx.enabled", "false");
    System.setProperty("spring.application.admin.enabled", "false");
  }

  /**
   * In this topology, each Connector starts-up with an Account for the other connector. During initialization,
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

        try {
          // Add Bob's account on Alice...
          final AccountSettingsEntity bobAccountSettingsAtAlice = constructBobAccountSettingsOnAlice(alicePort);
          aliceServerNode.getILPv4Connector().getAccountSettingsRepository().save(bobAccountSettingsAtAlice);

          // Add Alice's account on Bob...
          final AccountSettingsEntity aliceAccountSettingsAtBob = constructAliceAccountSettingsOnBob(bobPort);
          bobServerNode.getILPv4Connector().getAccountSettingsRepository().save(aliceAccountSettingsAtBob);

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

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
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
      "│  test.alice  │                     │   test.bob   │\n" +
      "│              │                     │              │\n" +
      "│              ├──────HTTP/2─────────▷              │\n" +
      "└──────────────┘                     └──────────────┘\n"
    );
    return topology;
  }

  /**
   * An AccountSettings object that represents Bob's account at Alice.
   *
   * @param bobPort The port that alice's server connects to in order to talk to Bob's server.
   */
  private static AccountSettingsEntity constructBobAccountSettingsOnAlice(final int bobPort) {
    return new AccountSettingsEntity(
      AccountSettings.builder()
        .accountId(AccountId.of(BOB))
        .description("Blast account for Bob")
        .accountRelationship(AccountRelationship.PEER)
        .rateLimitSettings(AccountRateLimitSettings.builder().maxPacketsPerSecond(5000).build())
        .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
        .linkType(BlastLink.LINK_TYPE)
        .assetScale(9)
        .assetCode(XRP)

        // Incoming
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT, BOB)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, "shh")
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, ALICE_TOKEN_ISSUER)
        //        .putCustomSettings(
        //          IncomingLinkSettings., "http://localhost:" + alicePort + IlpHttpController.ILP_PATH
        //        )

        // Outgoing
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, ALICE)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, "shh")
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
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Alice</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForAlice() {
    return ImmutableConnectorSettings.builder()
      .operatorAddress(ALICE_ADDRESS)
      .jwtTokenIssuer(ALICE_TOKEN_ISSUER)
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isBlastEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerRoutingEnabled(false)
        .isPeerConfigEnabled(true)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        .routingSecret("DocIHaveToTellYouSomethingAboutY")
        .build()
      )
      .build();
  }

  /**
   * An AccountSettings object that represents Alice's account at Bob.
   *
   * @param alicePort The port that bob's server connects to in order to talk to Alice's server.
   */
  private static AccountSettingsEntity constructAliceAccountSettingsOnBob(final int alicePort) {
    return new AccountSettingsEntity(
      AccountSettings.builder()
        .accountId(AccountId.of(ALICE))
        .description("Blast account for Alice")
        .rateLimitSettings(AccountRateLimitSettings.builder().maxPacketsPerSecond(5000).build())
        .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
        .accountRelationship(AccountRelationship.PEER)
        .linkType(BlastLink.LINK_TYPE)
        .assetScale(9)
        .assetCode(XRP)

        // Incoming
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT, ALICE)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, "shh")
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

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Bob</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForBob() {
    return ImmutableConnectorSettings.builder()
      .operatorAddress(BOB_ADDRESS)
      .jwtTokenIssuer(BOB_TOKEN_ISSUER)
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
  }

}
