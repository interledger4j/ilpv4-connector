package org.interledger.connector.server.ilphttp;

import static org.interledger.connector.server.spring.settings.blast.BlastConfig.BLAST;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.http.BlastHttpSender;
import org.interledger.connector.link.http.SimpleBearerBlastHttpSender;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.Decryptor;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Maps;
import okhttp3.HttpUrl;
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

/**
 * Ensures that the API endpoints for HTTP (i.e., `/ilp`) returns the correct values when a
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(
//  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//  classes = {ConnectorServerConfig.class}
//)
//@ActiveProfiles({"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class SimpleBearerBlastEndpointTest extends AbstractEndpointTest {

//  @LocalServerPort
//  int randomServerPort;
//
//  @Autowired
//  @Qualifier(BLAST)
//  RestTemplate blastRestTemplate;
//
//  @Autowired
//  Decryptor decryptor;
//
//  @Autowired
//  TestRestTemplate template;
//
//  @Autowired
//  AccountSettingsRepository accountSettingsRepository;
//
//  @Before
//  public void setUp() {
//    // Add the Bob Account to the Connector.
//    final Map<String, Object> customSettings = Maps.newHashMap();
//    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
//    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
//    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
//    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);
//
//    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
//    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
//    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, "https://bob.example.com/");
//    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
//    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
//    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://bob.example.com");
//
//    final AccountSettings accountSettings = AccountSettings.builder()
//        .accountId(AccountId.of("bob"))
//        .description("HTTP account for Bob using a simple shared-secret")
//        .accountRelationship(AccountRelationship.PEER)
//        .linkType(IlpOverHttpLink.LINK_TYPE)
//        .customSettings(customSettings)
//        .assetScale(2)
//        .assetCode("XRP")
//        .build();
//    accountSettingsRepository.save(new AccountSettingsEntity(accountSettings));
//  }
//
//  /**
//   * Validate the "test connection" method in the IL-DCP requestor.
//   */
//  @Test
//  @Ignore // Simple auth is not yet enabled in the code-base.
//  public void ildcpTestConnection() {
//    final BlastHttpSender blastHttpSender = simpleBearerBlastHttpSender();
//    blastHttpSender.testConnection();
//  }
//
//  private BlastHttpSender simpleBearerBlastHttpSender() {
//
//    final OutgoingLinkSettings outgoingLinkSettings = OutgoingLinkSettings.builder()
//        .authType(IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
//        .tokenSubject("bob")
//        .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
//        .tokenAudience(HttpUrl.parse("https://n-a.example.com"))
//        .url(HttpUrl.parse(template.getRootUri() + "/ilp"))
//        // The is the encrypted variant of `shh`
//        .encryptedTokenSharedSecret(ENCRYPTED_SHH)
//        .build();
//
//    return new SimpleBearerBlastHttpSender(
//        () -> Optional.of(InterledgerAddress.of("test.bob")),
//        blastRestTemplate,
//        outgoingLinkSettings,
//        decryptor
//    );
//  }

}
