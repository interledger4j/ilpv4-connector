package org.interledger.connector.it.topologies.settlement;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.it.topologies.AbstractTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledFeatureSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * <p>A very simple topology involving two Connectors that simulates ILP Settlement over XRP using a simulated
 * settlement engine.</p>
 *
 * <p>Nodes in this topology are connected as follows:</p>
 *
 * <pre>
 *                  ┌─────────┐                   ┌─────────┐                    ┌─────────┐
 *                  │   XRP   │                   │   XRP   │                    │   XRP   │
 *                  └─────────┘                   └─────────┘                    └─────────┘
 *
 *                            ┌──────────────┐                     ┌──────────────┐
 *                            │              │                     │              │
 * ┌─────────────────┐        │  CONNECTOR   │                     │  CONNECTOR   │        ┌─────────────────┐
 * │      Paul       │   ILP  │  test.alice  │                     │   test.bob   │   ILP  │      Peter      │
 * │(test.alice.paul)│◁─over─▷│ (port:8080)  │───Ilp-over-Http────▷│ (port: 8081) │◁─over─▷│(test.bob.peter) │
 * └─────────────────┘  HTTP  │              │                     │              │  HTTP  └─────────────────┘
 *                            │              │                     │              │
 *                            └──────────────┘                     └──────────────┘
 *                                    │                                    │
 *                                    ▼                                    ▼
 *                           ┌─────────────────┐                  ┌─────────────────┐
 *                           │Settlement Engine│                  │Settlement Engine│
 *                           │  (port: 9000)   │                  │  (port: 9001)   │
 *                           └─────────────────┘                  └─────────────────┘
 *                                    △             ┌─────────┐            △
 *                                    │             │   XRP   │            │
 *                                    └────────────▷│ Ledger  │◁───────────┘
 *                                                  └─────────┘
 * </pre>
 */
public class SimulatedXrplSettlementTopology extends AbstractTopology {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedXrplSettlementTopology.class);

  /**
   * In this topology, each Connector starts-up with an Account for the other connector. Each account is configured to
   * enable settlement using a simulated settlement engine.
   */
  public static Topology init(final int aliceContainerPort, final int bobContainerPort) {

    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
    final Topology topology = new Topology(SimulatedXrplSettlementTopology.class.getSimpleName(),
      new Topology.PostConstructListener() {
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
          bobServerNode.getILPv4Connector().getAccountSettingsRepository().deleteAll();

          // Add Paul's account on Alice (Paul is used for sending pings)
          final AccountSettings paulAccountSettingsAtAlice = constructPaulAccountSettingsOnAlice(alicePort);
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(paulAccountSettingsAtAlice);

          // Add Bob's account on Alice...
          final AccountSettings bobAccountSettingsAtAlice
            = constructBobAccountSettingsOnAlice(bobPort, aliceContainerPort);
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(bobAccountSettingsAtAlice);

          // Add Alice's account on Bob...
          final AccountSettings aliceAccountSettingsAtBob
            = constructAliceAccountSettingsOnBob(alicePort, bobContainerPort);
          bobServerNode.getILPv4Connector().getAccountManager().createAccount(aliceAccountSettingsAtBob);

          // Add Peter's account on Bob (Peter is used for sending pings)
          final AccountSettings peterAccountSettingsAtBob = constructPeterAccountSettingsOnBob(bobPort);
          bobServerNode.getILPv4Connector().getAccountManager().createAccount(peterAccountSettingsAtBob);

          // Add Ping account on Alice.
          // NOTE: The Connector configures a Ping Account properly but this Topology deletes all accounts above
          // before running, so we must create a new PING account here.
          final AccountSettings pingAccountSettings = constructPingAccountSettings();
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(pingAccountSettings);
          bobServerNode.getILPv4Connector().getAccountManager().createAccount(pingAccountSettings);

          // Try to connect the bob account...
          aliceServerNode.getILPv4Connector().getLinkManager().getOrCreateLink(bobAccountSettingsAtAlice);

          // Try to connect the alice account...
          bobServerNode.getILPv4Connector().getLinkManager().getOrCreateLink(aliceAccountSettingsAtBob);
        }
      });

    ///////////////////
    // Alice Connector Node
    ///////////////////
    {
      final ConnectorServer aliceServer = new ConnectorServer(constructConnectorSettingsForAlice());
      aliceServer.setPort(ALICE_PORT);
      aliceServer.setProperty("LOGGING_LEVEL", "TRACE");
      topology.addNode(
        ALICE_CONNECTOR_ADDRESS, new ConnectorServerNode(ALICE, aliceServer, constructStaticRoutesForAlice())
      );
    }

    ///////////////////
    // Bob Connector Node
    ///////////////////
    {
      final ConnectorServer bobServer = new ConnectorServer(constructConnectorSettingsForBob());
      bobServer.setPort(BOB_PORT);
      bobServer.setProperty("LOGGING_LEVEL", "TRACE");
      useH2(bobServer);
      topology.addNode(BOB_CONNECTOR_ADDRESS, new ConnectorServerNode(BOB, bobServer, constructStaticRoutesForBob()));
    }

    LOGGER.info("\n" +
      "                 ┌─────────┐                   ┌─────────┐                    ┌─────────┐                  \n" +
      "                 │   XRP   │                   │   XRP   │                    │   XRP   │                  \n" +
      "                 └─────────┘                   └─────────┘                    └─────────┘                  \n" +
      "                                                                                                           \n" +
      "                           ┌──────────────┐                     ┌──────────────┐                           \n" +
      "                           │              │                     │              │                           \n" +
      "┌─────────────────┐        │  CONNECTOR   │                     │  CONNECTOR   │        ┌─────────────────┐\n" +
      "│      Paul       │   ILP  │  test.alice  │                     │   test.bob   │   ILP  │      Peter      │\n" +
      "│(test.alice.paul)│◁─over─▷│ (port:8080)  │───Ilp-over-Http────▷│ (port: 8081) │◁─over─▷│(test.bob.peter) │\n" +
      "└─────────────────┘  HTTP  │              │                     │              │  HTTP  └─────────────────┘\n" +
      "                           │              │                     │              │                           \n" +
      "                           └──────────────┘                     └──────────────┘                           \n" +
      "                                   │                                    │                                  \n" +
      "                                   ▼                                    ▼                                  \n" +
      "                          ┌─────────────────┐                  ┌─────────────────┐                         \n" +
      "                          │Settlement Engine│                  │Settlement Engine│                         \n" +
      "                          │  (port: 9000)   │                  │  (port: 9001)   │                         \n" +
      "                          └─────────────────┘                  └─────────────────┘                         \n" +
      "                                   △             ┌─────────┐            △                                  \n" +
      "                                   │             │   XRP   │            │                                  \n" +
      "                                   └────────────▷│ Ledger  │◁───────────┘                                  \n" +
      "                                                 └─────────┘                                               "
    );
    return topology;
  }

  /**
   * An AccountSettings object that represents Bob's account at Alice.
   *
   * @param bobPort The port that alice's server connects to in order to talk to Bob's server.
   */
  private static AccountSettings constructBobAccountSettingsOnAlice(final int bobPort, final int containerPort) {
    return AccountSettings.builder()
      .accountId(BOB_ACCOUNT)
      .description("IlpOverHttp account for Bob")
      .accountRelationship(AccountRelationship.PEER)
      .settlementEngineDetails(
        SettlementEngineDetails.builder()
          .baseUrl(HttpUrl.parse("http://localhost:" + containerPort))
          .build()
      )
      .balanceSettings(
        AccountBalanceSettings.builder()
          .minBalance(-2000) // 2 XRP Drops if the scale is 6
          .settleThreshold(1000) // 1 XRP Drop if the scale is 6
          .settleTo(0)
          .build()
      )
      .maximumPacketAmount(UnsignedLong.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, BOB)
      //.putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
      //.putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, ALICE)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

      // Outgoing
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      //.putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, ALICE)
      //.putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, BOB)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_EXPIRY, EXPIRY_2MIN)
      .putCustomSettings(
        OutgoingLinkSettings.HTTP_OUTGOING_URL, createOutgoingLinkUrl(bobPort, ALICE_ACCOUNT)
      )

      .build();
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Alice</tt>.
   */
  private static ConnectorSettings constructConnectorSettingsForAlice() {
    return ImmutableConnectorSettings.builder()
      .operatorAddress(ALICE_CONNECTOR_ADDRESS)
      .enabledFeatures(EnabledFeatureSettings.builder()
        .isRequire32ByteSharedSecrets(false)
        .build())
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isIlpOverHttpEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerRoutingEnabled(false)
        .isIldcpEnabled(false)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
        .build()
      )
      .build();
  }

  private static Set<StaticRoute> constructStaticRoutesForAlice() {
    // Always route packets to Bob...
    return Sets.newHashSet(StaticRoute.builder()
      .routePrefix(InterledgerAddressPrefix.from(BOB_CONNECTOR_ADDRESS))
      .nextHopAccountId(BOB_ACCOUNT)
      .build()
    );
  }

  /**
   * An AccountSettings object that represents Alice's account at Bob.
   *
   * @param alicePort The port that bob's server connects to in order to talk to Alice's server.
   */
  private static AccountSettings constructAliceAccountSettingsOnBob(final int alicePort, final int containerPort) {
    return AccountSettings.builder()
      .accountId(ALICE_ACCOUNT)
      .description("ILP-over-HTTP account for Alice")
      .settlementEngineDetails(
        SettlementEngineDetails.builder()
          .baseUrl(HttpUrl.parse("http://localhost:" + containerPort))
          .build()
      )
      .balanceSettings(
        AccountBalanceSettings.builder()
          .minBalance(-2000) // 2 XRP Drops if the scale is 6
          .settleThreshold(1000) // 1 XRP Drop if the scale is 6
          .settleTo(0)
          .build()
      )
      .maximumPacketAmount(UnsignedLong.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .accountRelationship(AccountRelationship.PEER)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, ALICE)
      //.putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)
      //.putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, BOB)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

      // Outgoing
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, BOB)
      //.putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
      //.putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, ALICE)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_EXPIRY, EXPIRY_2MIN)
      .putCustomSettings(
        OutgoingLinkSettings.HTTP_OUTGOING_URL, createOutgoingLinkUrl(alicePort, BOB_ACCOUNT)
      )

      .build();
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Bob</tt>.
   */
  private static ConnectorSettings constructConnectorSettingsForBob() {
    return ImmutableConnectorSettings.builder()
      .operatorAddress(BOB_CONNECTOR_ADDRESS)
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
        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
        .build()
      )
      .build();
  }

  private static Set<StaticRoute> constructStaticRoutesForBob() {
    // Always route packets to Alice...
    return Sets.newHashSet(StaticRoute.builder()
      .routePrefix(InterledgerAddressPrefix.from(ALICE_CONNECTOR_ADDRESS))
      .nextHopAccountId(ALICE_ACCOUNT)
      .build()
    );
  }

  /**
   * An AccountSettings object that represents Paul's account at Alice. Since this account is only used to send, it does
   * not require any incoming connection settings.
   *
   * @param alicePort
   */
  private static AccountSettings constructPaulAccountSettingsOnAlice(int alicePort) {
    return AccountSettings.builder()
      .accountId(PAUL_ACCOUNT)
      .description("ILP-over-HTTP sender account for Paul")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, PAUL)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

      // Outgoing settings needed by testPing
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, PAUL)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(
        OutgoingLinkSettings.HTTP_OUTGOING_URL, createOutgoingLinkUrl(alicePort, PAUL_ACCOUNT)
      )

      .build();
  }

  /**
   * An AccountSettings object that represents Paul's account at Alice. Since this account is only used to send, it does
   * not require any incoming connection settings.
   */
  private static AccountSettings constructPeterAccountSettingsOnBob(int bobPort) {
    return AccountSettings.builder()
      .accountId(PETER_ACCOUNT)
      .description("ILP-over-HTTP sender account for Peter")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, PETER)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

      // Outgoing settings needed by testPing
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, PETER)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(
        OutgoingLinkSettings.HTTP_OUTGOING_URL, createOutgoingLinkUrl(bobPort, PETER_ACCOUNT)
      )

      .build();
  }

}
