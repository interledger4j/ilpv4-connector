package org.interledger.connector.balances;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;

import java.util.Objects;

/**
 * Services requests related to the Account Balance API
 */
public class AccountBalanceService {

  private final BalanceTracker balanceTracker;
  private final AccountSettingsRepository accountSettingsRepository;

  public AccountBalanceService(
    final BalanceTracker balanceTracker, final AccountSettingsRepository accountSettingsRepository
  ) {
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
  }

  /**
   * Find account balance for a specific accountId.
   *
   * @param accountId
   *
   * @return AccountBalanceResponse
   *
   * @throws AccountNotFoundProblem if accountId does not have any {@link AccountSettings}.
   */
  public AccountBalanceResponse getAccountBalance(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return accountSettingsRepository.findByAccountIdWithConversion(accountId)
      .map(settings ->
        AccountBalanceResponse.builder()
          .accountBalance(balanceTracker.balance(accountId))
          .assetCode(settings.assetCode())
          .assetScale((short) settings.assetScale())
          .build()
      ).orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

}
