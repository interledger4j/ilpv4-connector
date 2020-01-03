package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.client.ConnectorAdminTestClient;
import org.interledger.connector.server.spring.settings.Redactor;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.settlement.SettlementEngineClientException;
import org.interledger.connector.settlement.client.CreateSettlementAccountResponse;
import org.interledger.link.LoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import feign.FeignException;
import feign.RequestInterceptor;
import feign.Response;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;
import okhttp3.HttpUrl;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
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

  @Value("${interledger.connector.adminPassword}")
  private String adminPassword;

  private ConnectorAdminTestClient adminApiTestClient;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  private SettlementEngineAccountId mockSettlementEngineAccountId;

  @LocalServerPort
  private int localServerPort;

  @Before
  public void setUp() {

    final HttpUrl baseHttpUrl = HttpUrl.parse("http://localhost:" + localServerPort);
    final RequestInterceptor basicAuthRequestInterceptor = new BasicAuthRequestInterceptor("admin", adminPassword);
    adminApiTestClient = ConnectorAdminTestClient.construct(baseHttpUrl, basicAuthRequestInterceptor);

    mockSettlementEngineAccountId = SettlementEngineAccountId.of(UUID.randomUUID().toString());
    when(settlementEngineClientMock.createSettlementAccount(any(), any(), any()))
      .thenReturn(CreateSettlementAccountResponse.builder()
        .settlementEngineAccountId(mockSettlementEngineAccountId)
        .build()
      );
  }

  @Test
  public void testMinimalCreate() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    ImmutableAccountSettings settings = AccountSettings.builder()
      .accountId(accountId)
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("FUD")
      .assetScale(6)
      .linkType(LoopbackLink.LINK_TYPE)
      .createdAt(Instant.now())
      .build();

    AccountSettings response = assertPostAccountCreated(settings);
    assertThat(response).isEqualTo(settings);
  }

  @Test
  public void testDelete() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    AccountSettings settings = AccountSettings.builder()
      .accountId(accountId)
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("FUD")
      .assetScale(6)
      .linkType(LoopbackLink.LINK_TYPE)
      .createdAt(Instant.now())
      .build();

    AccountSettings response = assertPostAccountCreated(settings);
    assertThat(response).isEqualTo(settings);
    Optional<AccountSettings> retrieved = getAccount(accountId.value());
    assertThat(retrieved).isNotEmpty().get()
      .extracting("accountId", "assetCode")
      .containsExactly(settings.accountId(), settings.assetCode());

    final Response deleteResponse = adminApiTestClient.deleteAccountAsResponse(accountId.value());
    assertThat(deleteResponse.status()).isEqualTo(HttpStatus.NO_CONTENT.value());

    retrieved = getAccount(accountId.value());
    assertThat(retrieved).isEmpty();
  }

  @Test
  public void testDeleteUnknown404s() {
    String accountId = UUID.randomUUID().toString();
    adminApiTestClient.deleteAccountAsResponse(accountId);

    try {
      adminApiTestClient.deleteAccount(accountId);
      fail("Expected failure");
    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(404);

      JsonContentAssert assertJson = assertThat(jsonTester.from(e.content()));
      assertJson.extractingJsonPathValue("status").isEqualTo(404);
      assertJson.extractingJsonPathValue("title").isEqualTo("Account Not Found (`" + accountId + "`)");
      assertJson.extractingJsonPathValue("accountId").isEqualTo(accountId);
      assertJson.extractingJsonPathValue("type")
        .isEqualTo("https://errors.interledger.org/accounts/account-not-found");

    }
  }

  @Test
  public void testFullyPopulatedCreate() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final ImmutableAccountSettings settings = constructFullyPopulatedAccountSettings(accountId);

    AccountSettings response = assertPostAccountCreated(settings);
    assertThat(response).isEqualTo(settings);
  }

  @Test
  public void testFullyPopulatedCreateWithDuplicateSEAccountId() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final ImmutableAccountSettings accountSettings = constructFullyPopulatedAccountSettings(accountId);

    AccountSettings response = assertPostAccountCreated(accountSettings);
    assertThat(response).isEqualTo(accountSettings);

    // Same account details with different accountId but same SettlementAccountId
    final ImmutableAccountSettings newAccountSettingsWithDupSE = AccountSettings.builder().from(accountSettings)
      .accountId(AccountId.of(UUID.randomUUID().toString())).build();

    String rawResponse = assertPostAccountFailure(newAccountSettingsWithDupSE, HttpStatus.CONFLICT);
    JsonContentAssert assertJson = assertThat(jsonTester.from(rawResponse));
    assertJson.extractingJsonPathValue("status").isEqualTo(409);
    assertJson.extractingJsonPathValue("title")
      .isEqualTo("Account Settlement Engine Already Exists [accountId: `" +
        newAccountSettingsWithDupSE.accountId().value() + "`, settlementEngineId: `" +
        mockSettlementEngineAccountId.value() + "`]");
  }

  @Test
  public void testFullyPopulatedCreateWithSettlementEngineFailure() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    doThrow(new SettlementEngineClientException(
      "Unable to create account in settlement engine.", accountId, Optional.empty())
    ).when(settlementEngineClientMock).createSettlementAccount(any(), any(), any());

    final ImmutableAccountSettings settings = constructFullyPopulatedAccountSettings(accountId);

    String response = assertPostAccountFailure(settings, HttpStatus.INTERNAL_SERVER_ERROR);
    JsonContentAssert assertJson = assertThat(jsonTester.from(response));
    assertJson.extractingJsonPathValue("status").isEqualTo(500);
    assertJson.extractingJsonPathValue("title").isEqualTo("Internal Server Error");
    assertJson.extractingJsonPathValue("detail").isEqualTo("Unable to create account in settlement engine.");
  }

  @Test
  public void testCreateExistingIdReturns409() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    ImmutableAccountSettings settings = AccountSettings.builder()
      .accountId(accountId)
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("FUD")
      .assetScale(6)
      .linkType(LoopbackLink.LINK_TYPE)
      .createdAt(Instant.now())
      .customSettings(Maps.newHashMap("custom", "value"))
      .build();

    AccountSettings createResponse = assertPostAccountCreated(settings);
    assertThat(createResponse).isEqualTo(settings);

    // already exists
    String recreateResponse = assertPostAccountFailure(settings, HttpStatus.CONFLICT);
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
    ImmutableAccountSettings settings = AccountSettings.builder()
      .accountId(accountId)
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("FUD")
      .assetScale(6)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .createdAt(Instant.now())
      .customSettings(customSettings)
      .build();

    AccountSettings created = assertPostAccountCreated(settings);
    assertThat(created.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET))
      .isEqualTo(Redactor.REDACTED);
    assertThat(created.customSettings().get(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET))
      .isEqualTo(Redactor.REDACTED);
  }

  @Test
  public void createWithBadSecretFails() {
    String badEncodedSecret = "enc:JKS:crypto.p12:secret0:1:aes_gcm:!!!!!";

    Map<String, Object> customSettings = com.google.common.collect.Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, badEncodedSecret);

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, "https://bob.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, OUTGOING_SECRET);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://bob.example.com");

    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    ImmutableAccountSettings settings = AccountSettings.builder()
      .accountId(accountId)
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("FUD")
      .assetScale(6)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .createdAt(Instant.now())
      .customSettings(customSettings)
      .build();

    assertPostAccountFailure(settings, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void createWithColonInAccountIdFails() {
    // write out actual json since otherwise account id validation fails
    String json = "{" +
      "\"accountId\":\"pepe:silvia\"," +
      "\"createdAt\":\"+1000000000-12-31T23:59:59.999999999Z\"," +
      "\"modifiedAt\":\"+1000000000-12-31T23:59:59.999999999Z\"," +
      "\"description\":\"\"," +
      "\"accountRelationship\":\"PEER\"," +
      "\"assetCode\":\"USD\"," +
      "\"assetScale\":\"2\"," +
      "\"maximumPacketAmount\":null," +
      "\"linkType\":\"LOOPBACK\"," +
      "\"ilpAddressSegment\":\"bob\"," +
      "\"connectionInitiator\":true," +
      "\"internal\":false," +
      "\"sendRoutes\":false," +
      "\"receiveRoutes\":false," +
      "\"balanceSettings\":{" +
      "\"minBalance\":null," +
      "\"settleThreshold\":null," +
      "\"settleTo\":\"0\"" +
      "}," +
      "\"rateLimitSettings\":{" +
      "\"maxPacketsPerSecond\":null" +
      "}," +
      "\"settlementEngineDetails\":null," +
      "\"customSettings\":{}" +
      "}";

    try {
      adminApiTestClient.createAccountUsingJson(json);
      fail("Expected failure");
    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(400);
    }
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

  private AccountSettings assertPostAccountCreated(AccountSettings settings) throws IOException {
    Response response = adminApiTestClient.createAccountAsResponse(settings);
    assertThat(response.status()).isEqualTo(201);
    final Object decodedObject = new JacksonDecoder(ObjectMapperFactory.create())
      .decode(response, ImmutableAccountSettings.class);
    return (AccountSettings) decodedObject;
  }

  private String assertPostAccountFailure(ImmutableAccountSettings settings, HttpStatus expectedStatus) {
    try {
      adminApiTestClient.createAccount(settings);
    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(expectedStatus.value());
      return e.contentUTF8();
    }
    fail("Expected failure");
    return "not reachable";
  }

  private Optional<AccountSettings> getAccount(String accountId) {
    return adminApiTestClient.findAccount(accountId);
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
      .maximumPacketAmount(UnsignedLong.valueOf(200L))
      .isConnectionInitiator(true)
      .ilpAddressSegment("foo")
      .isSendRoutes(true)
      .isReceiveRoutes(true)
      .isInternal(true)
      .modifiedAt(Instant.MAX)
      .customSettings(Maps.newHashMap("custom", "value"))
      .build();
  }

}
