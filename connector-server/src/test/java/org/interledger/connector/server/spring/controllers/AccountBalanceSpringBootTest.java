package org.interledger.connector.server.spring.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.balances.AccountBalance;
import org.interledger.connector.balances.AccountBalanceResponse;
import org.interledger.connector.balances.AccountBalanceService;
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
import org.springframework.http.ResponseEntity;
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
  private AccountBalanceService accountBalanceService;

  @Test
  public void getBalanceRequiresAuthentication() {
    AccountId accountId = AccountId.of("john");
    expectedException.expect(FeignException.Unauthorized.class);
    userClient.getBalance(baseURI(), "", accountId.value());
  }

  @Test
  public void getBalance() {
    AccountId accountId = AccountId.of("john");
    AccountBalance balance = AccountBalance.builder()
      .accountId(accountId)
      .clearingBalance(1)
      .prepaidAmount(2)
      .build();

    AccountBalanceResponse expected = AccountBalanceResponse.builder()
      .accountBalance(balance)
      .assetScale((short) 9)
      .assetCode("XRP")
      .build();

    String password = "password";
    createAccount(accountId, customSettingsSimple(password));

    when(accountBalanceService.getAccountBalance(accountId)).thenReturn(expected);

    AccountBalanceResponse response = userClient.getBalance(baseURI(), bearer(password), accountId.value());
    assertThat(response).isEqualTo(expected);
  }

  @Test
  public void cantGetBalanceForDifferentUser() {
    AccountId jill = AccountId.of("jill");
    AccountId jane = AccountId.of("jane");
    String password = "password";
    createAccount(jill, customSettingsSimple(password));
    createAccount(jane, customSettingsSimple("differentpassword"));

    expectedException.expect(FeignException.Unauthorized.class);
    AccountBalanceResponse response = userClient.getBalance(baseURI(), bearer(password), jane.value());
  }

  private String bearer(String password) {
    return "Bearer " + password;
  }

  protected AccountSettings createAccount(AccountId accountId, Map<String, Object> customSettings) {
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .description("HTTP account for Bob using a simple shared-secret")
      .accountRelationship(AccountRelationship.PEER)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(customSettings)
      .assetScale(2)
      .assetCode("XRP")
      .build();

    ResponseEntity<ImmutableAccountSettings> result = adminClient.createAccount(baseURI(), accountSettings);
    return result.getBody();
  }

}