package org.interledger.connector.server.spring.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.balances.AccountBalance;
import org.interledger.connector.balances.AccountBalanceResponse;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.ilpoverhttp.AbstractEndpointTest;
import org.interledger.link.http.IlpOverHttpLink;

import feign.FeignException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"} )
public class AccountBalanceSpringBootTest extends AbstractEndpointTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @MockBean
  private BalanceTracker balanceTracker;

  @Test
  public void getBalanceRequiresAuthentication() {
    AccountId accountId = AccountId.of("napoleon");
    expectedException.expect(FeignException.Unauthorized.class);
    userClient.getBalance(baseURI(), "", accountId.value());
  }

  @Test
  public void getBalance() {
    AccountId accountId = AccountId.of("kip");
    String password = "password";
    AccountSettings settings = createAccount(accountId, customSettingsSimple(password));
    AccountBalance balance = AccountBalance.builder()
      .accountId(accountId)
      .clearingBalance(1)
      .prepaidAmount(2)
      .build();

    AccountBalanceResponse expected = AccountBalanceResponse.builder()
      .accountBalance(balance)
      .assetScale(settings.assetScale())
      .assetCode(settings.assetCode())
      .build();

    when(balanceTracker.balance(accountId)).thenReturn(balance);

    AccountBalanceResponse response = userClient.getBalance(baseURI(), bearer(password), accountId.value());
    assertThat(response).isEqualTo(expected);
  }

  @Test
  public void cantGetBalanceForDifferentUser() {
    AccountId pedro = AccountId.of("pedro");
    AccountId rex = AccountId.of("rex");
    String password = "password";
    createAccount(pedro, customSettingsSimple(password));
    createAccount(rex, customSettingsSimple("differentpassword"));

    expectedException.expect(FeignException.Unauthorized.class);
    AccountBalanceResponse response = userClient.getBalance(baseURI(), bearer(password), rex.value());
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