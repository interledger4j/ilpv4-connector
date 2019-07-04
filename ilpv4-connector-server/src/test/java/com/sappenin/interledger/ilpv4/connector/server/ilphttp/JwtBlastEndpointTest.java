package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServerConfig;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.blast.BlastHttpSender;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.ImmutableOutgoingLinkSettings;
import org.interledger.connector.link.blast.IncomingLinkSettings;
import org.interledger.connector.link.blast.JwtBlastHttpSender;
import org.interledger.connector.link.blast.OutgoingLinkSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.crypto.Decryptor;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink.LOOPBACK_FULFILLMENT;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig.BLAST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * Ensures that the API endpoints for BLAST (i.e., `/ilp`) returns the correct values. The Connector in this unit test
 * is `test.connie`, and this test simulates two accounts at the Connector, one for `alice` and one for `bob`.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles({"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class JwtBlastEndpointTest extends AbstractEndpointTest {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  @Qualifier(BLAST)
  RestTemplate blastRestTemplate;

  @Autowired
  Decryptor decryptor;

  @Autowired
  TestRestTemplate template;

  @Autowired
  AccountManager accountManager;

  @Before
  public void setup() {

    ////////////////
    // Add the Alice Account to the Connector.
    ////////////////

    if (!accountManager.getAccountSettingsRepository().findByAccountId(AccountId.of(ALICE)).isPresent()) {
      final Map<String, Object> customSettings = Maps.newHashMap();
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      //customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, "https://alice.example.com/");
      //customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);

      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      //customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
      //customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, "https://alice.example.com/");
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, CONNIE);
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_URL, "https://alice.example.com");

      final AccountSettings accountSettings = AccountSettings.builder()
        .accountId(AccountId.of("alice"))
        .description("Loopback account for Alice using a simple shared-secret")
        .accountRelationship(AccountRelationship.PEER)
        .linkType(LoopbackLink.LINK_TYPE)
        .customSettings(customSettings)
        .assetScale(2)
        .assetCode("XRP")
        .build();
      accountManager.createAccount(new AccountSettingsEntity(accountSettings));
    }

    ///////////////////////
    // Add the Bob Account to the Connector.
    ///////////////////////
    if (!accountManager.getAccountSettingsRepository().findByAccountId(AccountId.of(BOB)).isPresent()) {
      final Map<String, Object> customSettings = Maps.newHashMap();
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      //customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
      //customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);

      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      //customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
      //customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, "https://bob.example.com/");
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, CONNIE);
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_URL, "https://bob.example.com");

      final AccountSettings accountSettings = AccountSettings.builder()
        .accountId(AccountId.of("bob"))
        .description("BLAST account for Bob using a simple shared-secret")
        .accountRelationship(AccountRelationship.PEER)
        .linkType(BlastLink.LINK_TYPE)
        .customSettings(customSettings)
        .assetScale(2)
        .assetCode("XRP")
        .build();
      accountManager.createAccount(new AccountSettingsEntity(accountSettings));
    }
  }

  /**
   * Validate that Bob can send a packet to `alice` and get a fulfillment back. Since `alice` is operating a loopback
   * account, we always expect the {@link LoopbackLink#LOOPBACK_FULFILLMENT} to be returned, so we must make sure that
   * the prepare packets use the correct corresponding condition derived from that fulfillment.
   */
  @Test
  public void bobPaysAliceUsingIlpOverHttp() {
    final BlastHttpSender blastHttpSender = jwtBlastHttpSenderForBob();

    final InterledgerResponsePacket result = blastHttpSender.sendData(
      InterledgerPreparePacket.builder()
        .destination(InterledgerAddress.of("test.connie.alice"))
        .amount(BigInteger.ONE)
        .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
        .executionCondition(LOOPBACK_FULFILLMENT.getCondition())
        .build()
    );

    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(LOOPBACK_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail("Packet rejected but should not have!");
      }
    }.handle(result);
  }


  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void ildcpTestConnection() {
    final BlastHttpSender blastHttpSender = jwtBlastHttpSenderForBob();
    blastHttpSender.testConnection();
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Construct a new HTTP BLAST Client for the `bob` account
   */
  private BlastHttpSender jwtBlastHttpSenderForBob() {

    final OutgoingLinkSettings outgoingLinkSettings = ImmutableOutgoingLinkSettings.builder()
      .authType(BlastLinkSettings.AuthType.JWT_HS_256)
      .tokenSubject(BOB)
      .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
      .tokenAudience(HttpUrl.parse("https://connie.example.com/"))
      .url(HttpUrl.parse(template.getRootUri() + "/ilp"))
      .tokenExpiry(Duration.ofMinutes(5))
      .encryptedTokenSharedSecret(ENCRYPTED_SHH)
      .build();

    return new JwtBlastHttpSender(
      () -> Optional.of(InterledgerAddress.of("test.bob")),
      blastRestTemplate,
      decryptor, outgoingLinkSettings
    );
  }

}
