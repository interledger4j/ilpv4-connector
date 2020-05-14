package org.interledger.connector.it.topologies.ilpoverhttp;

import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.it.topologies.AbstractTopology;
import org.interledger.connector.it.topology.AbstractBaseTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.connector.opa.model.ImmutableOpenPaymentsSettings;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.SupportedAssets;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledFeatureSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.stream.Denomination;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

/**
 * <p>A very simple topology that simulates a single ILP-over-HTTP connection between two Connectors to enable a
 * pinging entity (Paul) to issue a ping request using his connector (test.alice) to ping the `test.bob` Connector. In
 * this way, Paul will pay units to Bob's ping account. .</p>
 *
 * <p>Nodes in this topology are connected as follows:</p>
 *
 * <pre>
 *                                       ┌──────────────┐                     ┌──────────────┐
 *                                       │              ◁───────HTTP/2────────┤              │
 * ┌─────────────────┐                   │              │                     │              │
 * │      Paul       │                   │  CONNECTOR   │                     │  CONNECTOR   │
 * │(test.alice.paul)│◁──Ilp-over-Http──▷│  test.alice  │                     │   test.bob   │
 * │                 │                   │              │                     │              │
 * └─────────────────┘                   │              ├──────HTTP/2─────────▷              │
 *                                       └──────────────┘                     └──────────────┘
 * </pre>
 */
public class TwoConnectorPeerIlpOverHttpTopology extends AbstractTopology {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorPeerIlpOverHttpTopology.class);

  /**
   * In this topology, each Connector starts-up with an Account for the other connector.
   *
   * @return The {@link Topology} of accounts and connectors
   */
  public static Topology init() {
    Denomination denomination = Denomination.builder().assetCode(XRP).assetScale((short) 9).build();
    return init(denomination, denomination, UnsignedLong.valueOf(1000000L)); // 1M NanoDollars is $0.001
  }

  /**
   * In this topology, each Connector starts-up with an Account for the other connector, specifying denominations and a
   * max packet amount.
   *
   * @return the topology of accounts and connectors
   */
  public static Topology init(final Denomination aliceBobDenomination, final Denomination paulAtAliceDenomination,
    final UnsignedLong maxPacketAmount) {

    // Some configuration must be done _after_ the topology starts...e.g., to grab the port that will be used.
    final Topology topology = new Topology(TwoConnectorPeerIlpOverHttpTopology.class.getSimpleName(),
      new AbstractBaseTopology.PostConstructListener<Topology>() {
        @Override
        protected void doAfterTopologyStartup(Topology g) {
          final ConnectorServerNode aliceServerNode =
            g.getNode(ALICE_CONNECTOR_ADDRESS.getValue(), ConnectorServerNode.class);
          final int alicePort = aliceServerNode.getPort();
          final ConnectorServerNode bobServerNode =
            g.getNode(BOB_CONNECTOR_ADDRESS.getValue(), ConnectorServerNode.class);
          final int bobPort = bobServerNode.getPort();

          // Delete all accounts before initializing the Topology otherwise we see sporadic CI build failures when
          // building on Postgres. This includes the "ping" account so that ping balances get reset from Topology to
          // Topology.
          aliceServerNode.getILPv4Connector().getAccountSettingsRepository().deleteAll();
          bobServerNode.getILPv4Connector().getAccountSettingsRepository().deleteAll();

          // Add Bob's account on Alice...
          final AccountSettings bobAccountSettingsAtAlice = constructBobAccountSettingsOnAlice(bobPort,
            aliceBobDenomination, maxPacketAmount);
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(bobAccountSettingsAtAlice);

          // Add Paul's account on Alice (Paul is used for sending pings)
          final AccountSettings paulAccountSettingsAtAlice = constructPaulAccountSettingsOnAlice(
            paulAtAliceDenomination, alicePort);
          aliceServerNode.getILPv4Connector().getAccountManager().createAccount(paulAccountSettingsAtAlice);

          // Add Peter's account on Bob (Peter is used for sending pings)
          final AccountSettings peterAccountSettingsAtBob = constructPeterAccountSettingsOnBob(bobPort);
          bobServerNode.getILPv4Connector().getAccountManager().createAccount(peterAccountSettingsAtBob);

          // Add Alice's account on Bob...
          final AccountSettings aliceAccountSettingsAtBob = constructAliceAccountSettingsOnBob(alicePort,
            aliceBobDenomination, maxPacketAmount);
          bobServerNode.getILPv4Connector().getAccountManager().createAccount(aliceAccountSettingsAtBob);

          // Add Ping account on Alice and Bob
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
      topology.addNode(ALICE_CONNECTOR_ADDRESS,
        new ConnectorServerNode(ALICE, aliceServer, constructStaticRoutesForAlice()));
    }

    ///////////////////
    // Bob Connector Node
    ///////////////////
    {
      final ConnectorServer bobServer = new ConnectorServer(constructConnectorSettingsForBob());
      bobServer.setPort(BOB_PORT);
      useH2(bobServer);
      topology.addNode(BOB_CONNECTOR_ADDRESS, new ConnectorServerNode(BOB, bobServer, constructStaticRoutesForBob()));
    }

    // FIXME we've added PETER at BOB so this needs to be updated
    LOGGER.info("\n" +
      "\nSTARTING ILP-OVER-HTTP TOPOLOGY\n" +
      "                                      ┌──────────────┐                     ┌──────────────┐\n" +
      "                                      │              ◁───────HTTP/2────────┤              │\n" +
      "┌─────────────────┐                   │              │                     │              │\n" +
      "│      Paul       │                   │  CONNECTOR   │                     │  CONNECTOR   │\n" +
      "│(test.alice.paul)│◁──Ilp-over-Http──▷│  test.alice  │                     │   test.bob   │\n" +
      "└─────────────────┘                   │              │                     │              │\n" +
      "                                      │              ├──────HTTP/2─────────▷              │\n" +
      "                                      └──────────────┘                     └──────────────┘"
    );
    return topology;
  }

  /**
   * An AccountSettings object that represents Bob's account at Alice.
   *
   * @param bobPort The port that alice's server connects to in order to talk to Bob's server.
   */
  private static AccountSettings constructBobAccountSettingsOnAlice(final int bobPort, final Denomination denomination,
    final UnsignedLong maxPacketAmount) {
    return AccountSettings.builder()
      .accountId(BOB_ACCOUNT)
      .description("ILP-over-HTTP account for Bob")
      .accountRelationship(AccountRelationship.PEER)
      .rateLimitSettings(AccountRateLimitSettings.builder().maxPacketsPerSecond(5000).build())
      .maximumPacketAmount(maxPacketAmount)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(denomination.assetScale())
      .assetCode(denomination.assetCode())

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      //.putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
      //.putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, ALICE)
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
   * An AccountSettings object that represents Paul's account at Alice. Since this account is only used to send, it does
   * not require any incoming connection settings.
   */
  private static AccountSettings constructPaulAccountSettingsOnAlice(final Denomination denomination, int alicePort) {
    return AccountSettings.builder()
      .accountId(PAUL_ACCOUNT)
      .description("ILP-over-HTTP sender account for Paul")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(denomination.assetScale())
      .assetCode(denomination.assetCode())

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

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Alice</tt>.
   */
  private static ConnectorSettings constructConnectorSettingsForAlice() {
    return ImmutableConnectorSettings.builder()
      .operatorAddress(ALICE_CONNECTOR_ADDRESS)
      .enabledFeatures(EnabledFeatureSettings.builder()
        .isRequire32ByteSharedSecrets(false)
        .isLocalSpspFulfillmentEnabled(true)
        .build())
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isIlpOverHttpEnabled(true)
        .isPingProtocolEnabled(true)
        .isPeerRoutingEnabled(false)
        .isIldcpEnabled(false)
        .isSpspEnabled(true)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
        .build()
      )
      .openPayments(constructOpenPaymentsSettings(ALICE_CONNECTOR_ADDRESS, ALICE_PORT, ALICE_HTTP_BASE_URL))
      .build();
  }

  private static ImmutableOpenPaymentsSettings constructOpenPaymentsSettings(
    InterledgerAddress interledgerAddress,
    int port,
    String connectorUrl) {
    return OpenPaymentsSettings.builder()
      .ilpOperatorAddress(interledgerAddress)
      .connectorUrl(HttpUrl.parse("http://localhost:" + port))
      .metadata(
        OpenPaymentsMetadata.builder()
          .assetsSupported(Collections.singleton(SupportedAssets.XRP))
          .issuer(HttpUrl.get("http://localhost:" + port))
          .defaultScheme("http")
          .build()
      )
      .connectorUrl(HttpUrl.get(connectorUrl))
      .build();
  }

  private static Set<StaticRoute> constructStaticRoutesForAlice() {
    // Always route packets to Bob...
    return Sets.newHashSet(StaticRoute.builder()
      .routePrefix(InterledgerAddressPrefix.from(BOB_CONNECTOR_ADDRESS))
      .nextHopAccountId(BOB_ACCOUNT)
      .build());
  }

  /**
   * An AccountSettings object that represents Alice's account at Bob.
   *
   * @param alicePort The port that bob's server connects to in order to talk to Alice's server.
   */
  private static AccountSettings constructAliceAccountSettingsOnBob(final int alicePort,
    final Denomination denomination,
    final UnsignedLong maxPacketAmount) {
    return AccountSettings.builder()
      .accountId(ALICE_ACCOUNT)
      .description("ILP-over-HTTP account for Alice")
      .rateLimitSettings(AccountRateLimitSettings.builder().maxPacketsPerSecond(5000).build())
      .maximumPacketAmount(maxPacketAmount)
      .accountRelationship(AccountRelationship.PEER)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(denomination.assetScale())
      .assetCode(denomination.assetCode())

      // Incoming
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, ALICE)
      .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH)

      // Outgoing
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
      .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, BOB)
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
        .isLocalSpspFulfillmentEnabled(true)
        .build())
      .enabledProtocols(EnabledProtocolSettings.builder()
        .isIlpOverHttpEnabled(true)
        .isPingProtocolEnabled(true)
        .isIldcpEnabled(false)
        .isPeerRoutingEnabled(false)
        .isSpspEnabled(true)
        .build())
      .globalPrefix(InterledgerAddressPrefix.TEST)
      .globalRoutingSettings(GlobalRoutingSettings.builder()
        //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
        // plaintext value of `shh`, encrypted.
        .routingSecret("enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
        .build()
      )
      .openPayments(constructOpenPaymentsSettings(BOB_CONNECTOR_ADDRESS, BOB_PORT, BOB_HTTP_BASE_URL))
      .build();
  }

  private static Set<StaticRoute> constructStaticRoutesForBob() {
    // Always route packets to Alice...
    return Sets.newHashSet(
      StaticRoute.builder()
      .routePrefix(InterledgerAddressPrefix.from(ALICE_CONNECTOR_ADDRESS))
      .nextHopAccountId(ALICE_ACCOUNT)
      .build(),
      StaticRoute.builder()
      .routePrefix(InterledgerAddressPrefix.from(PETER_AT_BOB_ADDRESS))
      .nextHopAccountId(PETER_ACCOUNT)
      .build()
    );
  }

}
