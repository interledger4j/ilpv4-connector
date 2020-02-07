package org.interledger.connector.it.topologies.ilpoverhttp;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.it.topologies.AbstractTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledFeatureSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.connector.settings.ModifiableConnectorSettings;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A very simple topology that simulates a single ILP-over-HTTP connection between two Connectors where one
 * Connector (`alice`) is the <tt>Parent</tt>, and the other Connector (`bob`) is the child.</p>
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
public class TwoConnectorParentChildIlpOverHttpTopology extends AbstractTopology {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorParentChildIlpOverHttpTopology.class);

  /**
   * In this topology, each Connector starts-up with an Account for the other connector.
   *
   * @return A {@link Topology}.
   */
  public static Topology init() {

    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
    final Topology topology = new Topology(TwoConnectorParentChildIlpOverHttpTopology.class.getSimpleName(),
      new Topology.PostConstructListener() {

        @Override
        protected void doAfterTopologyStartup(Topology g) {
          final ConnectorServerNode aliceServerNode = g.getNode(
            ALICE_CONNECTOR_ADDRESS.getValue(), ConnectorServerNode.class
          );
          final int alicePort = aliceServerNode.getPort();

          final ConnectorServerNode bobServerNode = g.getNode(
            BOB_AT_ALICE_ADDRESS.getValue(), ConnectorServerNode.class
          );
          final int bobPort = bobServerNode.getPort();

          // Delete all accounts before initializing the Topology otherwise we see sporadic CI build failures when
          // building on Postgres. This includes the "ping" account so that ping balances get reset from Topology to
          // Topology.
          aliceServerNode.getILPv4Connector().getAccountSettingsRepository().deleteAll();
          bobServerNode.getILPv4Connector().getAccountSettingsRepository().deleteAll();

          // Add Ping account on Alice and Bob
          // NOTE: The Connector configures a Ping Account properly but this Topology deletes all accounts above
          // before running, so we must create a new PING account here.
          final AccountSettings pingAccountSettings = constructPingAccountSettings();
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(pingAccountSettings);
          bobServerNode.getILPv4Connector().getAccountManager().createAccount(pingAccountSettings);

          // Add Bob's account on Alice...
          final AccountSettings bobAccountSettingsAtAlice = constructBobAccountSettingsOnAlice(bobPort);
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(bobAccountSettingsAtAlice);

          // Add Alice's account on Bob...
          final AccountSettings aliceAccountSettingsAtBob = constructAliceAccountSettingsOnBob(alicePort);
          // Will perform IL-DCP since the type is `PARENT`
          bobServerNode.getILPv4Connector().getAccountManager().createAccount(aliceAccountSettingsAtBob);
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
      useH2(bobServer);
      topology.addNode(BOB_AT_ALICE_ADDRESS, new ConnectorServerNode(BOB, bobServer));
    }

    LOGGER.info("\n" +
      "\nSTARTING ILP-OVER-HTTP TOPOLOGY\n" +
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
  private static ConnectorSettings constructConnectorSettingsForAlice() {
    final ConnectorSettings connectorSettings = ImmutableConnectorSettings.builder()
      .operatorAddress(ALICE_CONNECTOR_ADDRESS)
      .enabledFeatures(EnabledFeatureSettings.builder()
        .isRequire32ByteSharedSecrets(false)
        .build())
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isIlpOverHttpEnabled(true)
        .isPingProtocolEnabled(true)
        .isIldcpEnabled(true)
        .isPeerRoutingEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret(ENCRYPTED_SHH)
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
  private static AccountSettings constructBobAccountSettingsOnAlice(final int bobPort) {
    return AccountSettings.builder()
      .accountId(AccountId.of(BOB))
      .description("ILP-over-HTTP account for Bob @ Alice")
      .accountRelationship(AccountRelationship.CHILD)
      .maximumPacketAmount(UnsignedLong.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .isConnectionInitiator(true)
      .assetScale(9)
      .assetCode(XRP)

      // Alice, the Connector Account operator, has an account that she manages, allowing Bob to connect. This
      // account has an id of `bob` (i.e., Bob's account with Alice). Thus, the incoming account-id for this link
      // will be `bob`. The token-issuer in this case will also be Bob, since he is generating the token for the
      // incoming request.

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, BOB)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

      // Outgoing
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, ALICE)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_EXPIRY, EXPIRY_2MIN)
      .putCustomSettings(
        OutgoingLinkSettings.HTTP_OUTGOING_URL, createOutgoingLinkUrl(bobPort, ALICE_ACCOUNT)
      )

      .build();
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Bob</tt>.
   */
  private static ConnectorSettings constructConnectorSettingsForBob() {
    final ConnectorSettings connectorSettings = ImmutableConnectorSettings.builder()
      .enabledFeatures(EnabledFeatureSettings.builder()
        .isRequire32ByteSharedSecrets(false)
        .build())
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isIlpOverHttpEnabled(true)
        .isPingProtocolEnabled(true)
        .isIldcpEnabled(false)
        .isPeerRoutingEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret(ENCRYPTED_SHH)
        .build()
      )
      .build();

    // Must be modifiable to support IL-DCP
    return ModifiableConnectorSettings.create().from(connectorSettings);
  }

  private static AccountSettings constructAliceAccountSettingsOnBob(final int alicePort) {
    return AccountSettings.builder()
      .accountId(AccountId.of(ALICE))
      .description("ILP-over-HTTP account for Alice @ Bob")
      .isConnectionInitiator(true)
      .maximumPacketAmount(UnsignedLong.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .accountRelationship(AccountRelationship.PARENT)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      // Bob, the Connector Account operator, has an account that he manages, allowing Alice to connect. This account
      // has an id of `alice` (i.e., Alice's account with Bob). Thus, the incoming account-id for this link will be
      // `alice`. The token-issuer in this case will also be Alice, since she is generating the token for the
      // incoming request.

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, BOB)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, ALICE)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

      // Outgoing
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, ALICE)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, BOB)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_EXPIRY, EXPIRY_2MIN)
      .putCustomSettings(
        OutgoingLinkSettings.HTTP_OUTGOING_URL, createOutgoingLinkUrl(alicePort, BOB_ACCOUNT)
      )
      .build();
  }

}
