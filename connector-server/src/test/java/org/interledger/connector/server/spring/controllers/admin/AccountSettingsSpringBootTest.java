package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.links.loopback.LoopbackLink;
import org.interledger.connector.server.ConnectorServerConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;
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

  @Autowired
  TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  /**
   * Verify settings can be created via API
   */
  @Test
  public void testCreate() throws IOException {
    AccountSettings settings = AccountSettings.builder()
        .accountId(AccountId.of("testCreate"))
        .accountRelationship(AccountRelationship.CHILD)
        .assetCode("FUD")
        .assetScale(6)
        .linkType(LoopbackLink.LINK_TYPE)
        .createdAt(Instant.now())
        .customSettings(Maps.newHashMap("custom", "value"))
        .build();

    String response = assertPostAccount(settings, HttpStatus.CREATED);
    assertThat(as(response, ImmutableAccountSettings.class)).isEqualTo(settings);
  }

  /**
   * Verify settings can be created via API
   */
  @Test
  public void testCreateExistingIdReturns409() throws IOException {

    AccountId accountId = AccountId.of("testCreateExistingIdReturns409");
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
    assertJson.extractingJsonPathValue("title").isEqualTo("Account Already Exists (`testCreateExistingIdReturns409`)");
    assertJson.extractingJsonPathValue("accountId").isEqualTo(accountId.value());
    assertJson.extractingJsonPathValue("type")
        .isEqualTo("https://errors.interledger.org/accounts/account-already-exists");
  }

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

  /**
   * Verify API ignores unknown properties
   */
  @Test
  public void testJsonMarshalling() {
    final HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(ADMIN, PASSWORD);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> rawValues = ImmutableMap.<String, Object>builder()
        .put("accountId", AccountId.of("testJsonMarshalling"))
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

  private <T> T as(String value, Class<T> toClass) throws IOException {
    return objectMapper.readValue(value, toClass);
  }

}
