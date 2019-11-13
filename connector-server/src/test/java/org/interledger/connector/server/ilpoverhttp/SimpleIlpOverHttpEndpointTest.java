package org.interledger.connector.server.ilpoverhttp;

import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.LinkId;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Base64;
import java.util.Map;

/**
 * Ensures that the API endpoints for ILP-over-HTTP (i.e., `/ilp`) return the correct values for various test scenarios
 * using * the `SIMPLE` profile defined in the Interledger HTTP Authentication recommendations RFC.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class SimpleIlpOverHttpEndpointTest extends AbstractEndpointTest {

  private static final String ACCOUNT_ID = "bob:ross";
  private static final String SECRET = Base64.getEncoder().encodeToString("shh".getBytes());
  private static final String BAD_SECRET = Base64.getEncoder().encodeToString("pfft".getBytes());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient okHttpClient;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TestRestTemplate template;

  @Autowired
  private AccountSettingsRepository accountSettingsRepository;

  @Before
  public void setUp() {
    AccountId bob = AccountId.of(ACCOUNT_ID);
    if (!accountSettingsRepository.findByAccountId(bob).isPresent()) {
      createAccount(bob);
    }
  }

  private void createAccount(AccountId accountId) {
    // Add the Bob Account to the Connector.
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, "https://bob.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://bob.example.com");

    final AccountSettings accountSettings = AccountSettings.builder()
        .accountId(accountId)
        .description("HTTP account for Bob using a simple shared-secret")
        .accountRelationship(AccountRelationship.PEER)
        .linkType(IlpOverHttpLink.LINK_TYPE)
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
  public void ildcpTestConnection() {
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(ENCRYPTED_SHH, ACCOUNT_ID + ":" + SECRET);
    simpleBearerLink.setLinkId(LinkId.of(ACCOUNT_ID));
    assertLink(simpleBearerLink);
  }

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void incorrectTokenCredentials() {
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(ENCRYPTED_SHH, ACCOUNT_ID + ":" + BAD_SECRET);
    simpleBearerLink.setLinkId(LinkId.of(ACCOUNT_ID));
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Unauthorized");
    assertLink(simpleBearerLink);
  }

  private IlpOverHttpLink simpleBearerLink(String sharedSecret, String bearerToken) {

    final OutgoingLinkSettings outgoingLinkSettings = OutgoingLinkSettings.builder()
        .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
        .tokenSubject("bob")
        .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
        .tokenAudience(HttpUrl.parse("https://n-a.example.com"))
        .url(HttpUrl.parse(template.getRootUri() + "/ilp"))
        // The is the encrypted variant of `shh`
        .encryptedTokenSharedSecret(sharedSecret)
        .build();

    final IncomingLinkSettings incomingLinkSettings = IncomingLinkSettings.builder()
        .encryptedTokenSharedSecret(sharedSecret)
        .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
        .tokenIssuer(outgoingLinkSettings.tokenIssuer())
        .tokenAudience(outgoingLinkSettings.tokenAudience())
        .build();

    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(incomingLinkSettings)
        .outgoingHttpLinkSettings(outgoingLinkSettings)
        .build();

    return new IlpOverHttpLink(
        () -> InterledgerAddress.of("test.bob"),
        linkSettings,
        okHttpClient,
        objectMapper,
        InterledgerCodecContextFactory.oer(),
        new SimpleBearerTokenSupplier(bearerToken)
    );

  }

  private void assertLink(IlpOverHttpLink simpleBearerLink) {
    simpleBearerLink.testConnection();
  }

}
