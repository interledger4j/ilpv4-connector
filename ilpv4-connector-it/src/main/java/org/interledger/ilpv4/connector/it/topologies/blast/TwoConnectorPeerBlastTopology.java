package org.interledger.ilpv4.connector.it.topologies.blast;

import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.IlpHttpController;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
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
import java.util.concurrent.TimeUnit;

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
          {
            // Try to re-connect the link...
            final Account bobAccount = aliceServerNode.getILPv4Connector().getAccountManager()
              .getAccount(AccountId.of(BOB)).get();
            bobAccount.getLink().connect().get(5, TimeUnit.SECONDS);
          }
          {
            // Try to re-connect the link...
            final Account aliceAccount =
              bobServerNode.getILPv4Connector().getAccountManager().getAccount(AccountId.of(ALICE)).get();
            aliceAccount.getLink().connect().get(5, TimeUnit.SECONDS);
          }
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
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Alice</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForAlice() {

    final AccountSettings accountSettings = AccountSettings.builder()
      .id(AccountId.of(BOB))
      .description("Blast account for Bob")
      .isPreconfigured(true)
      .relationship(AccountRelationship.PEER)
      .rateLimitSettings(AccountRateLimitSettings.builder().maxPacketsPerSecond(5000).build())
      .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .linkType(BlastLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_ID, BOB)
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)

      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_ID, ALICE)
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, "PT2M")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, BOB_TOKEN_ISSUER)
      .putCustomSettings(
        BlastLinkSettings.BLAST_OUTGOING_URL,
        "http://localhost:" + BOB_PORT + IlpHttpController.ILP_PATH
      )
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT.name())

      .build();

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
      .addAccountSettings(
        // Must be modifiable so that we can update these settings after the Topology starts (to assign ports).
        // This type of non-immutability is only used for tests like this one, and should not be used in production.
        ModifiableAccountSettings.create().from(accountSettings)
      )
      .build();
  }

  /**
   * Construct a {@link ConnectorSettings} with a Connector properly configured to represent <tt>Bob</tt>.
   */
  public static ConnectorSettings constructConnectorSettingsForBob() {

    final AccountSettings accountSettings = AccountSettings.builder()
      .id(AccountId.of(ALICE))
      .description("Blast account for Alice")
      .isPreconfigured(true)
      .rateLimitSettings(AccountRateLimitSettings.builder().maxPacketsPerSecond(5000).build())
      .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .relationship(AccountRelationship.PEER)
      .linkType(BlastLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_ID, ALICE)
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)

      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_ID, BOB)
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER) // Replaced by
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, "PT2M")
      .putCustomSettings(
        BlastLinkSettings.BLAST_OUTGOING_URL,
        "http://localhost:" + ALICE_PORT + IlpHttpController.ILP_PATH
      )
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT.name())

      .build();

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
      .addAccountSettings(
        // Must be modifiable so that we can update these settings after the Topology starts (to assign ports).
        // This type of non-immutability is only used for tests like this one, and should not be used in production.
        ModifiableAccountSettings.create().from(accountSettings)
      )
      .build();
  }

}
