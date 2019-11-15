package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.settlement.SettlementEngineClientException;
import org.interledger.connector.settlement.client.CreateSettlementAccountResponse;
import org.interledger.link.LoopbackLink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import okhttp3.HttpUrl;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
@ActiveProfiles( {"test"})
public class AccountSettingsSpringBootTest {

  private static final String PASSWORD = "password";
  private static final String ADMIN = "admin";

  @MockBean
  private SettlementEngineClient settlementEngineClientMock;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());
  
  private SettlementEngineAccountId mockSettlementEngineAccountId;

  @Before
  public void setUp() {
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
  public void testFullyPopulatedCreateWithDuplicateSEAccountId() throws IOException {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final AccountSettings accountSettings = constructFullyPopulatedAccountSettings(accountId);

    String response = assertPostAccount(accountSettings, HttpStatus.CREATED);
    assertThat(as(response, ImmutableAccountSettings.class)).isEqualTo(accountSettings);

    // Same account details with different accountId but same SettlementAccountId
    final AccountSettings newAccountSettingsWithDupSE = AccountSettings.builder().from(accountSettings)
        .accountId(AccountId.of(UUID.randomUUID().toString())).build();

    response = assertPostAccount(newAccountSettingsWithDupSE, HttpStatus.CONFLICT);
    JsonContentAssert assertJson = assertThat(jsonTester.from(response));
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
}
