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
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.client.ConnectorAdminClient;
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
import okhttp3.HttpUrl;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
@EnableFeignClients(clients = ConnectorAdminClient.class)
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
  private ConnectorAdminClient adminClient;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  @Autowired
  private TestRestTemplate restTemplate;

  @LocalServerPort
  private int localServerPort;

  private URI baseURI;

  @Before
  public void setUp() throws URISyntaxException {
    baseURI = new URI("http://localhost:" + localServerPort);
    when(settlementEngineClientMock.createSettlementAccount(any(), any(), any()))
      .thenReturn(CreateSettlementAccountResponse.builder()
        .settlementEngineAccountId(SettlementEngineAccountId.of(UUID.randomUUID().toString()))
        .build()
      );
  }

  @Test
  public void testMinimalCreate() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    AccountSettings settings = AccountSettings.builder()
      .accountId(accountId)
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("FUD")
      .assetScale(6)
      .linkType(LoopbackLink.LINK_TYPE)
      .createdAt(Instant.now())
      .build();

    AccountSettings response = assertPostAccount(settings, HttpStatus.CREATED);
    assertThat(response).isEqualTo(settings);
  }

  @Test
  public void testFullyPopulatedCreate() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final AccountSettings settings = constructFullyPopulatedAccountSettings(accountId);

    AccountSettings response = assertPostAccount(settings, HttpStatus.CREATED);
    assertThat(response).isEqualTo(settings);
  }

  @Test
  @Ignore("Will be fixed once https://github.com/sappenin/java-ilpv4-connector/issues/416 is fixed.")
  public void testFullyPopulatedCreateWithDuplicateSEAccountId() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final AccountSettings accountSettings = constructFullyPopulatedAccountSettings(accountId);

    AccountSettings response = assertPostAccount(accountSettings, HttpStatus.CREATED);
    assertThat(response).isEqualTo(accountSettings);

    // Same account details with different accountId but same SettlementAccountId
    final AccountSettings newAccountSettingsWithDupSE = AccountSettings.builder().from(accountSettings)
      .accountId(AccountId.of(UUID.randomUUID().toString())).build();

    response = assertPostAccount(newAccountSettingsWithDupSE, HttpStatus.CONFLICT);
    assertThat(response).isEqualTo(accountSettings);
  }

  @Test
  public void testFullyPopulatedCreateWithSettlementEngineFailure() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    doThrow(new SettlementEngineClientException(
      "Unable to create account in settlement engine.", accountId, Optional.empty())
    ).when(settlementEngineClientMock).createSettlementAccount(any(), any(), any());

    final AccountSettings settings = constructFullyPopulatedAccountSettings(accountId);

    String response = assertPostAccountFailure(settings, HttpStatus.INTERNAL_SERVER_ERROR);
    JsonContentAssert assertJson = assertThat(jsonTester.from(response));
    assertJson.extractingJsonPathValue("status").isEqualTo(500);
    assertJson.extractingJsonPathValue("title").isEqualTo("Internal Server Error");
    assertJson.extractingJsonPathValue("detail").isEqualTo("Unable to create account in settlement engine.");
  }

  @Test
  public void testCreateExistingIdReturns409() {
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

    AccountSettings createResponse = assertPostAccount(settings, HttpStatus.CREATED);
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
  public void testCreateAndAuthAccountWithSimple() {
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

    AccountSettings created = assertPostAccount(settings, HttpStatus.CREATED);
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
    AccountSettings settings = AccountSettings.builder()
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

  private AccountSettings assertPostAccount(AccountSettings settings, HttpStatus expectedStatus) {
    ResponseEntity<ImmutableAccountSettings> response = adminClient.createAccount(baseURI, settings);
    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    return response.getBody();
  }

  private String assertPostAccountFailure(AccountSettings settings, HttpStatus expectedStatus) {
    try {
      adminClient.createAccount(baseURI, settings);
    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(expectedStatus.value());
      return e.contentUTF8();
    }
    fail("Expected failure");
    return "not reachable";
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
