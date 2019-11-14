package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static org.interledger.connector.server.spring.settings.blast.IlpOverHttpConfig.BLAST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.settlement.SettlementEngineClientException;
import org.interledger.connector.settlement.client.CreateSettlementAccountResponse;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.LinkId;
import org.interledger.link.LoopbackLink;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Running-server test that validates behavior of account settings resource.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test", "jks"})
public class AccountSettingsSpringBootTest {

  private static final String PASSWORD = "password";
  private static final String ADMIN = "admin";

  private static final String INCOMING_SECRET = Base64.getEncoder().encodeToString("shh".getBytes());
  private static final String OUTGOING_SECRET = Base64.getEncoder().encodeToString("hush".getBytes());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @MockBean
  private SettlementEngineClient settlementEngineClientMock;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  @Qualifier(BLAST)
  private OkHttpClient okHttpClient;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  @Before
  public void setUp() {
    when(settlementEngineClientMock.createSettlementAccount(any(), any(), any()))
        .thenReturn(CreateSettlementAccountResponse.builder()
            .settlementEngineAccountId(SettlementEngineAccountId.of(UUID.randomUUID().toString()))
            .build()
        );
  }

  @Test
  public void testMinimalCreate() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    AccountSettings settings = AccountSettings.builder()
        .accountId(accountId)
        .accountRelationship(AccountRelationship.CHILD)
        .assetCode("FUD")
        .assetScale(6)
        .linkType(LoopbackLink.LINK_TYPE)
        .createdAt(Instant.now())
        .build();

    String response = assertPostAccount(settings, HttpStatus.CREATED);
    assertThat(as(response, ImmutableAccountSettings.class)).isEqualTo(settings);
  }

  @Test
  public void testFullyPopulatedCreate() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final AccountSettings settings = constructFullyPopulatedAccountSettings(accountId);

    String response = assertPostAccount(settings, HttpStatus.CREATED);
    assertThat(as(response, ImmutableAccountSettings.class)).isEqualTo(settings);
  }

  @Test
  @Ignore("Will be fixed once https://github.com/sappenin/java-ilpv4-connector/issues/416 is fixed.")
  public void testFullyPopulatedCreateWithDuplicateSEAccountId() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final AccountSettings accountSettings = constructFullyPopulatedAccountSettings(accountId);

    String response = assertPostAccount(accountSettings, HttpStatus.CREATED);
    assertThat(as(response, ImmutableAccountSettings.class)).isEqualTo(accountSettings);

    // Same account details with different accountId but same SettlementAccountId
    final AccountSettings newAccountSettingsWithDupSE = AccountSettings.builder().from(accountSettings)
        .accountId(AccountId.of(UUID.randomUUID().toString())).build();

    response = assertPostAccount(newAccountSettingsWithDupSE, HttpStatus.CONFLICT);
    assertThat(as(response, AccountSettings.class)).isEqualTo(accountSettings);
  }

  @Test
  public void testFullyPopulatedCreateWithSettlementEngineFailure() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    doThrow(new SettlementEngineClientException(
        "Unable to create account in settlement engine.", accountId, Optional.empty())
    ).when(settlementEngineClientMock).createSettlementAccount(any(), any(), any());

    final AccountSettings settings = constructFullyPopulatedAccountSettings(accountId);

    String response = assertPostAccount(settings, HttpStatus.INTERNAL_SERVER_ERROR);
    JsonContentAssert assertJson = assertThat(jsonTester.from(response));
    assertJson.extractingJsonPathValue("status").isEqualTo("500");
    assertJson.extractingJsonPathValue("title").isEqualTo("Internal Server Error");
    assertJson.extractingJsonPathValue("detail").isEqualTo("Unable to create account in settlement engine.");
  }

  @Test
  public void testCreateExistingIdReturns409() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    AccountSettings settings = AccountSettings.builder()
        .accountId(accountId)
        .accountRelationship(AccountRelationship.CHILD)
        .assetCode("FUD")
        .assetScale(6)
        .linkType(LoopbackLink.LINK_TYPE)
        .createdAt(Instant.now())
        .customSettings(Maps.newHashMap("custom", "value"))
        .build();

    String createResponse = assertPostAccount(settings, HttpStatus.CREATED);
    assertThat(as(createResponse, ImmutableAccountSettings.class)).isEqualTo(settings);

    // already exists
    String recreateResponse = assertPostAccount(settings, HttpStatus.CONFLICT);
    JsonContentAssert assertJson = assertThat(jsonTester.from(recreateResponse));
    assertJson.extractingJsonPathValue("status").isEqualTo(409);
    assertJson.extractingJsonPathValue("title").isEqualTo("Account Already Exists (`" + accountId.value() + "`)");
    assertJson.extractingJsonPathValue("accountId").isEqualTo(accountId.value());
    assertJson.extractingJsonPathValue("type")
        .isEqualTo("https://errors.interledger.org/accounts/account-already-exists");
  }


  @Test
  public void testCreateAndAuthAccountWithSimple() throws IOException {
    Map<String, Object> customSettings = com.google.common.collect.Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, INCOMING_SECRET);

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, "https://bob.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, OUTGOING_SECRET);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://bob.example.com");

    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    AccountSettings settings = AccountSettings.builder()
        .accountId(accountId)
        .accountRelationship(AccountRelationship.CHILD)
        .assetCode("FUD")
        .assetScale(6)
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .createdAt(Instant.now())
        .customSettings(customSettings)
        .build();

    AccountSettings created = as(assertPostAccount(settings, HttpStatus.CREATED), AccountSettings.class);
    assertThat(created.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET))
        .isEqualTo("REDACTED");

    String bearerToken = accountId + ":" + INCOMING_SECRET;
    String badNewsBearerToken = accountId + ":" + OUTGOING_SECRET;

    assertLink(simpleBearerLink(INCOMING_SECRET, bearerToken));

    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Unauthorized");
    assertLink(simpleBearerLink(INCOMING_SECRET, badNewsBearerToken));
  }

  /**
   * Verify API ignores unknown properties
   */
  @Test
  public void testJsonMarshalling() {
    final HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(ADMIN, PASSWORD);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> rawValues = ImmutableMap.<String, Object>builder()
        .put("accountId", AccountId.of(UUID.randomUUID().toString()))
        .put("accountRelationship", AccountRelationship.CHILD)
        .put("assetCode", "FUD")
        .put("assetScale", 6)
        .put("linkType", LoopbackLink.LINK_TYPE)
        .put("whatHasTwoThumbsAndLikesInterledger", "this guy") // random unknown property, should be ignored by server
        .build();

    final HttpEntity httpEntity = new HttpEntity(rawValues, headers);

    ResponseEntity response =
        restTemplate.exchange(SLASH_ACCOUNTS, HttpMethod.POST, httpEntity, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  //////////////////
  // Private Helpers
  //////////////////

  private String assertPostAccount(AccountSettings settings, HttpStatus expectedStatus) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(ADMIN, PASSWORD);
    headers.setContentType(MediaType.APPLICATION_JSON);
    final HttpEntity httpEntity = new HttpEntity(settings, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(SLASH_ACCOUNTS, httpEntity, String.class);
    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    return response.getBody();
  }

  private ImmutableAccountSettings constructFullyPopulatedAccountSettings(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    return AccountSettings.builder()
        .accountId(accountId)
        .description("A fully-populated account")
        .accountRelationship(AccountRelationship.CHILD)
        .assetCode("FUD")
        .assetScale(6)
        .linkType(LoopbackLink.LINK_TYPE)
        .createdAt(Instant.now())
        .balanceSettings(AccountBalanceSettings.builder()
            .settleThreshold(10L)
            .minBalance(9L)
            .settleTo(8L)
            .build())
        .settlementEngineDetails(SettlementEngineDetails.builder()
            .baseUrl(HttpUrl.parse("http://example.com"))
            .settlementEngineAccountId(SettlementEngineAccountId.of(UUID.randomUUID().toString()))
            .putCustomSettings("settlementFoo", "settlementBar")
            .build())
        .rateLimitSettings(AccountRateLimitSettings.builder()
            .maxPacketsPerSecond(100)
            .build())
        .maximumPacketAmount(200L)
        .isConnectionInitiator(true)
        .ilpAddressSegment("foo")
        .isSendRoutes(true)
        .isReceiveRoutes(true)
        .isInternal(true)
        .modifiedAt(Instant.MAX)
        .customSettings(Maps.newHashMap("custom", "value"))
        .build();
  }

  private <T> T as(String value, Class<T> toClass) throws IOException {
    return objectMapper.readValue(value, toClass);
  }

  private IlpOverHttpLink simpleBearerLink(String sharedSecret, String bearerToken) {

    final OutgoingLinkSettings outgoingLinkSettings = OutgoingLinkSettings.builder()
        .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
        .tokenSubject("bob")
        .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
        .tokenAudience(HttpUrl.parse("https://n-a.example.com"))
        .url(HttpUrl.parse(restTemplate.getRootUri() + "/ilp"))
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

    IlpOverHttpLink link = new IlpOverHttpLink(
        () -> InterledgerAddress.of("test.bob"),
        linkSettings,
        okHttpClient,
        objectMapper,
        InterledgerCodecContextFactory.oer(),
        new SimpleBearerTokenSupplier(bearerToken)
    );
    link.setLinkId(LinkId.of(bearerToken));
    return link;
  }

  private void assertLink(IlpOverHttpLink simpleBearerLink) {
    simpleBearerLink.testConnection();
  }
}
