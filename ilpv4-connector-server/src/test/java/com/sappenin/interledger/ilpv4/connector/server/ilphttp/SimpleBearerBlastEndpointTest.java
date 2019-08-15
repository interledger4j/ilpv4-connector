package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.google.common.collect.Maps;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.blast.BlastHttpSender;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.ImmutableOutgoingLinkSettings;
import org.interledger.connector.link.blast.IncomingLinkSettings;
import org.interledger.connector.link.blast.OutgoingLinkSettings;
import org.interledger.connector.link.blast.SimpleBearerBlastHttpSender;
import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.Decryptor;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig.BLAST;

/**
 * Ensures that the API endpoints for BLAST (i.e., `/ilp`) returns the correct values when a
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(
//  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//  classes = {ConnectorServerConfig.class}
//)
//@ActiveProfiles({"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class SimpleBearerBlastEndpointTest extends AbstractEndpointTest {

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
  public void setUp() {
    // Add the Bob Account to the Connector.
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);

    customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.SIMPLE.name());
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

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  @Ignore // Simple auth is not yet enabled in the code-base.
  public void ildcpTestConnection() {
    final BlastHttpSender blastHttpSender = simpleBearerBlastHttpSender();
    blastHttpSender.testConnection();
  }

  private BlastHttpSender simpleBearerBlastHttpSender() {

    final OutgoingLinkSettings outgoingLinkSettings = ImmutableOutgoingLinkSettings.builder()
      .authType(BlastLinkSettings.AuthType.JWT_HS_256)
      .tokenSubject("bob")
      .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
      .tokenAudience(HttpUrl.parse("https://n-a.example.com"))
      .url(HttpUrl.parse(template.getRootUri() + "/ilp"))
      // The is the encrypted variant of `shh`
      .encryptedTokenSharedSecret(ENCRYPTED_SHH)
      .build();

    return new SimpleBearerBlastHttpSender(
      () -> Optional.of(InterledgerAddress.of("test.bob")),
      blastRestTemplate,
      outgoingLinkSettings,
      decryptor
    );
  }

}
