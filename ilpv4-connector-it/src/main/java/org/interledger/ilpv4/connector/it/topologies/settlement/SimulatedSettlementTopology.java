package org.interledger.ilpv4.connector.it.topologies.settlement;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.StaticRoute;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.IlpHttpController;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.IncomingLinkSettings;
import org.interledger.connector.link.blast.OutgoingLinkSettings;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.ilpv4.connector.it.topologies.AbstractTopology;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.interledger.ilpv4.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A very simple topology involving two Connectors that simulates ILP Settlement over XRP using a simulated
 * settlement engine.</p>
 *
 * <p>Nodes in this topology are connected as follows:</p>
 *
 * <pre>
 *                     ┌─────────┐
 *                     │   XRP   │
 *                     └─────────┘
 *
 * ┌──────────────┐                     ┌──────────────┐
 * │              ◁───────HTTP/2────────┤              │
 * │              │                     │              │
 * │  CONNECTOR   │                     │  CONNECTOR   │
 * │  test.alice  │                     │   test.bob   │
 * │              │                     │              │
 * │              ├──────HTTP/2─────────▷              │
 * └──────────────┘                     └──────────────┘
 *         │                                    │
 *         │                                    │
 *         │                                    │
 *         │      ┌──────────────────────┐      │
 *         │      │  Settlement Engine   │      │
 *         └─────▶│     (Simulated)      │◀─────┘
 *                └──────────────────────┘
 * </pre>
 */
public class SimulatedSettlementTopology extends AbstractTopology {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedSettlementTopology.class);

  /**
   * In this topology, each Connector starts-up with an Account for the other connector. Each account is configured to
   * enable settlement using a simulated settlement engine.
   */
  public static Topology init() {

    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
    final Topology topology = new Topology(new Topology.PostConstructListener() {
      @Override
      protected void doAfterTopologyStartup(Topology g) {
        final ConnectorServerNode aliceServerNode =
          g.getNode(ALICE_CONNECTOR_ADDRESS.getValue(), ConnectorServerNode.class);
        final int alicePort = aliceServerNode.getPort();
        final ConnectorServerNode bobServerNode =
          g.getNode(BOB_CONNECTOR_ADDRESS.getValue(), ConnectorServerNode.class);
        final int bobPort = bobServerNode.getPort();

        // Delete all accounts before initializing the Topology otherwise we see sporadic CI build failures when
        // building on Postgres. Only need to do this on one server since both servers share the same DB.
        aliceServerNode.getILPv4Connector().getAccountSettingsRepository().deleteAll();

        try {
          // Add Bob's account on Alice...
          final AccountSettingsEntity bobAccountSettingsAtAlice = constructBobAccountSettingsOnAlice(bobPort);
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(bobAccountSettingsAtAlice);

          // Add Alice's account on Bob...
          final AccountSettingsEntity aliceAccountSettingsAtBob = constructAliceAccountSettingsOnBob(alicePort);
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(aliceAccountSettingsAtBob);

          // Add Ping account on Alice (Bob and Alice share a DB here, so this will work for Bob too).
          // NOTE: The Connector configures a Ping Account properly but this Topology deletes all accounts above
          // before running, so we must create a new PING account here.
          final AccountSettingsEntity pingAccountSettingsAtBob = constructPingAccountSettings();
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(pingAccountSettingsAtBob);

          // Try to connect the bob account...
          aliceServerNode.getILPv4Connector().getLinkManager().getOrCreateLink(bobAccountSettingsAtAlice);

          // Try to connect the alice account...
          bobServerNode.getILPv4Connector().getLinkManager().getOrCreateLink(aliceAccountSettingsAtBob);

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
      topology.addNode(ALICE_CONNECTOR_ADDRESS, new ConnectorServerNode(ALICE, aliceServer));
    }

    ///////////////////
    // Bob Connector Node
    ///////////////////
    {
      final ConnectorServer bobServer = new ConnectorServer(constructConnectorSettingsForBob());
      bobServer.setPort(BOB_PORT);
      topology.addNode(BOB_CONNECTOR_ADDRESS, new ConnectorServerNode(BOB, bobServer));
    }

    LOGGER.info("\n" +
      "\nSTARTING SETTLEMENT TOPOLOGY:                      \n" +
      "                    ┌─────────┐                      \n" +
      "                    │   XRP   │                      \n" +
      "                    └─────────┘                      \n" +
      "                                                     \n" +
      "┌──────────────┐                     ┌──────────────┐\n" +
      "│              ◁───────HTTP/2────────┤              │\n" +
      "│              │                     │              │\n" +
      "│  CONNECTOR   │                     │  CONNECTOR   │\n" +
      "│  test.alice  │                     │   test.bob   │\n" +
      "│              │                     │              │\n" +
      "│              ├──────HTTP/2─────────▷              │\n" +
      "└──────────────┘                     └──────────────┘\n" +
      "        │                                    │       \n" +
      "        │                                    │       \n" +
      "        │                                    │       \n" +
      "        │      ┌──────────────────────┐      │       \n" +
      "        │      │  Settlement Engine   │      │       \n" +
      "        └─────▶│     (Simulated)      │◀─────┘       \n" +
      "               └──────────────────────┘              "
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
        .accountId(BOB_ACCOUNT)
        .description("IlpOverHttp account for Bob")
        .accountRelationship(AccountRelationship.PEER)
        .settlementEngineDetails(
          SettlementEngineDetails.builder()
            .assetScale(6)
            .settlementEngineAccountId(BOB_ACCOUNT.value())
            .baseUrl(HttpUrl.parse("http://localhost:9000"))
            .build()
        )
        .maximumPacketAmount(1000000L) // 1M NanoDollars is $0.001
        .linkType(BlastLink.LINK_TYPE)
        .assetScale(9)
        .assetCode(XRP)

        // Incoming
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        //.putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
        //.putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, ALICE)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

        // Outgoing
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        //.putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, ALICE)
        //.putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, BOB)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, EXPIRY_2MIN)
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
      .operatorAddress(ALICE_CONNECTOR_ADDRESS)
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isBlastEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerRoutingEnabled(false)
        .isPeerConfigEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
        // Always route packets to Bob...
        .staticRoutes(Lists.newArrayList(StaticRoute.builder()
          .targetPrefix(InterledgerAddressPrefix.from(BOB_CONNECTOR_ADDRESS))
          .peerAccountId(BOB_ACCOUNT)
          .build()
        ))
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
        .accountId(ALICE_ACCOUNT)
        .description("Blast account for Alice")
        .settlementEngineDetails(
          SettlementEngineDetails.builder()
            .assetScale(6)
            .settlementEngineAccountId(BOB_ACCOUNT.value())
            .baseUrl(HttpUrl.parse("http://localhost:9000"))
            .build()
        )
        .maximumPacketAmount(1000000L) // 1M NanoDollars is $0.001
        .accountRelationship(AccountRelationship.PEER)
        .linkType(BlastLink.LINK_TYPE)
        .assetScale(9)
        .assetCode(XRP)

        // Incoming
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        //.putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)
        //.putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, BOB)
        .putCustomSettings(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

        // Outgoing
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256)
        //.putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, BOB)
        //.putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, ALICE)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
        .putCustomSettings(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, EXPIRY_2MIN)
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
      .operatorAddress(BOB_CONNECTOR_ADDRESS)
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isBlastEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerConfigEnabled(false)
        .isPeerRoutingEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
        // Always route packets to Alice...
        .staticRoutes(Lists.newArrayList(StaticRoute.builder()
          .targetPrefix(InterledgerAddressPrefix.from(ALICE_CONNECTOR_ADDRESS))
          .peerAccountId(ALICE_ACCOUNT)
          .build()
        ))
        .build()
      )
      .build();
  }

}