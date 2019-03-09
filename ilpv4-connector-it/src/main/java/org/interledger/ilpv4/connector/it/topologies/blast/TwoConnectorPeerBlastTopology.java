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
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ModifiableAccountSettings;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.ModifiableBlastLinkSettings;
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
 * <p>In this topology, Alice and Bob share a single XRP account (meaning Alice and Bob can owe each other XRP).</p>
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
            final Account bobAccount = aliceServerNode.getILPv4Connector().getAccountManager()
              .getAccount(AccountId.of(BOB)).get();

            // Need to reach-into the AccountManager and adjust the outgoing URL for Bob, based upon the newly discovered
            // port in `bobBlastUrl`
            final HttpUrl bobBlastUrl = HttpUrl.parse("http://localhost:" + bobPort + IlpHttpController.ILP_PATH);
            final ModifiableBlastLinkSettings modifiableBlastLinkSettings = (ModifiableBlastLinkSettings)
              bobAccount.getLink().getLinkSettings();
            modifiableBlastLinkSettings.setOutgoingUrl(bobBlastUrl);
            ((BlastLink) bobAccount.getLink()).reconfigure(
              () -> aliceServerNode.getILPv4Connector().getNodeIlpAddress(),
              modifiableBlastLinkSettings
            );

            // Try to re-connect the link...
            bobAccount.getLink().connect().get(5, TimeUnit.SECONDS);
          }
          {
            final Account aliceAccount = bobServerNode.getILPv4Connector().getAccountManager()
              .getAccount(AccountId.of(ALICE)).get();

            // Need to reach-into the AccountManager and adjust the outgoing URL for Alice, based upon the newly
            // discovered port in `aliceBlastUrl`
            final HttpUrl aliceBlastUrl = HttpUrl.parse("http://localhost:" + alicePort + IlpHttpController.ILP_PATH);
            final ModifiableBlastLinkSettings modifiableBlastLinkSettings = (ModifiableBlastLinkSettings)
              aliceAccount.getLink().getLinkSettings();
            modifiableBlastLinkSettings.setOutgoingUrl(aliceBlastUrl);
            ((BlastLink) aliceAccount.getLink()).reconfigure(
              () -> bobServerNode.getILPv4Connector().getNodeIlpAddress(),
              modifiableBlastLinkSettings
            );

            // Try to re-connect the link...
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
      topology.addNode(ALICE_ADDRESS,
        new ConnectorServerNode(ALICE, new ConnectorServer(constructConnectorSettingsForAlice())));
    }

    ///////////////////
    // Bob Connector Node
    ///////////////////
    {
      topology
        .addNode(BOB_ADDRESS, new ConnectorServerNode(BOB, new ConnectorServer(constructConnectorSettingsForBob())));
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
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_URL, "http://example.com/set-after-topology-init")
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
      .maximumPacketAmount(BigInteger.valueOf(1000000L)) // 1M NanoDollars is $0.001
      .relationship(AccountRelationship.PEER)
      .linkType(BlastLink.LINK_TYPE)
      .assetScale(9)
      .assetCode(XRP)

      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_ID, ALICE)
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, ALICE_TOKEN_ISSUER)

      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_ID, BOB)
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_SECRET, "12345678912345678912345678912345")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, "PT2M")
      .putCustomSettings(BlastLinkSettings.BLAST_OUTGOING_URL, "http://example.com/set-after-topology-init")

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
