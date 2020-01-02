package org.interledger.connector.balances;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.stream.Denomination;

/**
 * Services requests related to the Account Balance API
 */
public class AccountBalanceService {

  private final BalanceTracker balanceTracker;
  private final AccountSettingsRepository accountSettingsRepository;

  public AccountBalanceService(BalanceTracker balanceTracker, AccountSettingsRepository accountSettingsRepository) {
    this.balanceTracker = balanceTracker;
    this.accountSettingsRepository = accountSettingsRepository;
  }

  /**
   * Find account balance for a specific accountId.
   *
   * @param accountId
   * @return balance
   *
   * @throws AccountNotFoundProblem if accountId does not have any {@link org.interledger.connector.accounts.AccountSettings}
   */
  public AccountBalanceResponse getAccountBalance(AccountId accountId) {
    return accountSettingsRepository.findByAccountIdWithConversion(accountId)
      .map(settings ->
        AccountBalanceResponse.builder()
          .accountBalance(balanceTracker.balance(accountId))
          .denomination(Denomination.builder()
            .assetCode(settings.assetCode())
            .assetScale((short) settings.assetScale())
            .build()
          )
          .build()
      ).orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

}
