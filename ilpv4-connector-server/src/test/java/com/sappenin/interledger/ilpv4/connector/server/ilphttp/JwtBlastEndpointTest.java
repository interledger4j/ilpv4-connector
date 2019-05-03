package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServerConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
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
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.crypto.Decryptor;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig.BLAST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD;
import static org.junit.Assert.fail;

/**
 * Ensures that the API endpoints for BLAST (i.e., `/ilp`) returns the correct values. The Connector in this unit test
 * is `test1.alice`.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles({"test"})
@TestPropertySource(
  properties = {
    ConnectorProperties.ENABLED_PROTOCOLS + "." + ConnectorProperties.BLAST_ENABLED + "=true",
    ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME + "=crypto/crypto.p12",
    ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD + "=password",
    ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS + "=secret0",
    ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD + "=password"
  }
)
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
  AccountSettingsRepository accountSettingsRepository;

  @Before
  public void setup() {

    ////////////////
    // Add the Alice Account to the Connector.
    ////////////////
    {
      final Map<String, Object> customSettings = Maps.newHashMap();
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, "https://alice.example.com/");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT, "alice");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);

      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, "https://alice.example.com/");
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, "connie");
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
      accountSettingsRepository.save(new AccountSettingsEntity(accountSettings));
    }

    ///////////////////////
    // Add the Bob Account to the Connector.
    ///////////////////////
    {
      final Map<String, Object> customSettings = Maps.newHashMap();
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT, "bob");
      customSettings.put(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);

      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, "https://bob.example.com/");
      customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, "connie");
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
      accountSettingsRepository.save(new AccountSettingsEntity(accountSettings));
    }
  }

  /**
   * Validate that Bob can send a packet to `alice` and get a fulfillment back. Since `alice` is operating a loopback
   * account, we always expect a fulfillment.
   */
  @Test
  public void bobPaysAliceUsingIlpOverHttp() {
    final BlastHttpSender blastHttpSender = jwtBlastHttpSender();

    final InterledgerResponsePacket result = blastHttpSender.sendData(
      InterledgerPreparePacket.builder()
        .destination(InterledgerAddress.of("test.alice"))
        .amount(BigInteger.ONE)
        .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .build()
    );

    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(LoopbackLink.LOOPBACK_FULFILLMENT));
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
    final BlastHttpSender blastHttpSender = jwtBlastHttpSender();
    blastHttpSender.testConnection();
  }

  /**
   * Setup the HTTP BLAST Client for the `bob` account
   */
  private BlastHttpSender jwtBlastHttpSender() {

    final OutgoingLinkSettings outgoingLinkSettings = ImmutableOutgoingLinkSettings.builder()
      .authType(BlastLinkSettings.AuthType.JWT_HS_256)
      .tokenSubject("bob")
      .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
      .tokenAudience("https://connie.example.com/")
      .url(HttpUrl.parse(template.getRootUri() + "/ilp"))
      .tokenExpiry(Duration.ofMinutes(5))
      .encryptedTokenSharedSecret(ENCRYPTED_SHH)
      .build();

    return new JwtBlastHttpSender(
      () -> Optional.of(InterledgerAddress.of("test.bob")),
      blastRestTemplate,
      outgoingLinkSettings,
      decryptor
    );
  }

}