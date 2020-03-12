package org.interledger.connector.server.spring.controllers.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.interledger.connector.accounts.AccessToken;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.balances.AccountBalanceResponse;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.ilpoverhttp.AbstractEndpointTest;
import org.interledger.link.http.IlpOverHttpLink;

import feign.FeignException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"} )
public class AccountAccessTokenSpringBootTest extends AbstractEndpointTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createGeneratedToken() {
    AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    String password = "password";
    createAccount(accountId, customSettingsSimple(password));

    AccessToken response =
      userClient.createToken(bearer(password), accountId);

    assertThat(response.rawToken()).isPresent();

    assertTokenWorks(accountId, response.rawToken().get());
  }

  @Test
  public void deleteSingleToken() {
    AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    String password = "password";
    createAccount(accountId, customSettingsSimple(password));

    AccessToken token1 =
      userClient.createToken(bearer(password), accountId);

    AccessToken token2 =
      userClient.createToken(bearer(password), accountId);

    assertThat(userClient.getTokens(bearer(password), accountId)).hasSize(2);

    userClient.deleteToken(bearer(password), accountId, token2.id());
    assertTokenNotAuthorized(accountId, token2.rawToken().get());

    assertThat(userClient.getTokens(bearer(password), accountId)).hasSize(1);

    assertTokenWorks(accountId, token1.rawToken().get());

    userClient.deleteToken(bearer(password), accountId, token1.id());

    assertTokenNotAuthorized(accountId, token1.rawToken().get());
  }

  @Test
  public void deleteAll() {
    AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    String password = "password";
    createAccount(accountId, customSettingsSimple(password));

    int createTokenCount = 3;
    for(int i = 0; i < createTokenCount; i++) {
      String generatedToken = createToken(accountId, password);
      assertTokenWorks(accountId, generatedToken);
    }
    List<AccessToken> tokens = userClient.getTokens(bearer(password), accountId);
    assertThat(tokens).hasSize(createTokenCount);
    assertThat(tokens).extracting(AccessToken::rawToken).containsOnly(Optional.empty());
    assertThat(tokens).extracting(AccessToken::encryptedToken).doesNotHaveDuplicates();

    userClient.deleteTokens(bearer(password), accountId);

    assertThat(userClient.getTokens(bearer(password), accountId)).isEmpty();
  }

  @Test
  public void cantAccessOtherUsersTokens() {
    AccountId accountId1 = AccountId.of(UUID.randomUUID().toString());
    AccountId accountId2 = AccountId.of(UUID.randomUUID().toString());
    String password1 = "hello my lady";
    String password2 = "hello my darling";
    createAccount(accountId1, customSettingsSimple(password1));
    createAccount(accountId2, customSettingsSimple(password2));

    String account1Token = createToken(accountId1, password1);
    String account2Token = createToken(accountId2, password2);

    assertTokenWorks(accountId1, account1Token);
    assertTokenWorks(accountId2, account2Token);

    // check token doesn't work on other accounts
    assertTokenNotAuthorized(accountId1, account2Token);
    assertTokenNotAuthorized(accountId2, account1Token);

    assertThat(userClient.getTokens(account1Token, accountId1))
      .hasSize(1)
      .extracting(AccessToken::accountId).containsOnly(accountId1);

    assertThat(userClient.getTokens(account2Token, accountId2))
      .hasSize(1)
      .extracting(AccessToken::accountId).containsOnly(accountId2);
  }

  private String createToken(AccountId accountId, String password) {
    AccessToken response =
      userClient.createToken(bearer(password), accountId);
    assertThat(response.rawToken()).isPresent();
    return response.rawToken().get();
  }

  private void assertTokenWorks(AccountId accountId, String myToken) {
    AccountBalanceResponse response = userClient.getBalance(bearer(myToken), accountId.value());
    assertThat(response.accountBalance()).isNotNull();
  }

  private void assertTokenNotAuthorized(AccountId accountId, String myToken) {
    try {
      userClient.getBalance(bearer(myToken), accountId.value());
      fail("authorization exception excepted");
    } catch(FeignException e) {
      assertThat(e).isInstanceOf(FeignException.Unauthorized.class);
    }
  }

  private String bearer(String password) {
    return "Bearer " + password;
  }

  protected AccountSettings createAccount(AccountId accountId, Map<String, Object> customSettings) {
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .description("unit test account")
      .accountRelationship(AccountRelationship.PEER)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(customSettings)
      .assetScale(2)
      .assetCode("XRP")
      .build();

    return adminApiTestClient.createAccount(accountSettings);
  }

}