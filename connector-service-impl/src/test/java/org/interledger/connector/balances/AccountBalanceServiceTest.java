package org.interledger.connector.balances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.link.http.IlpOverHttpLink;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Optional;

public class AccountBalanceServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private AccountSettingsRepository mockSettingsRepo;

  @Mock
  private BalanceTracker mockBalanceTracker;

  private AccountBalanceService service;

  @Before
  public void setUp() {
    initMocks(this);
    service = new AccountBalanceService(mockBalanceTracker, mockSettingsRepo);
  }

  @Test
  public void getAccountBalance() {
    AccountId accountId = AccountId.of("test");
    AccountSettings settings =  AccountSettings.builder()
      .accountId(accountId)
      .description("child")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetScale(2)
      .assetCode("XRP")
      .build();

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

    when(mockSettingsRepo.findByAccountIdWithConversion(accountId)).thenReturn(Optional.of(settings));
    when(mockBalanceTracker.balance(accountId)).thenReturn(balance);

    AccountBalanceResponse response = service.getAccountBalance(accountId);
    assertThat(response).isEqualTo(expected);
  }

  @Test
  public void getAccountBalanceAccountNotFound() {
    AccountId accountId = AccountId.of("test");
    when(mockSettingsRepo.findByAccountIdWithConversion(accountId)).thenReturn(Optional.empty());
    expectedException.expect(AccountNotFoundProblem.class);
    service.getAccountBalance(accountId);
  }


}