package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;

import java.util.Optional;

/**
 * <p>Manages settings for any account operated by this Connector.</p>
 *
 * <p>The idea here is that various portions of an overall Account require different handling with respect to
 * performance guarantees (balances vs. Link details vs Account Settings). Thus, this manager handles the underlying
 * configuration settings for a given account, and allows them to be mutated by external systems, such as the account
 * owner or an administrative action.</p>
 */
public interface AccountSettingsManager {

  /**
   * Create an {@link Account} using the supplied {@code accountSettings}. The account won't
   *
   * @param accountSettings
   */
  AccountSettings createAccountSettings(AccountSettings accountSettings);

  /**
   * Update an account's settings with new values.
   *
   * @param accountSettings The new settings to use for this account.
   *
   * @return The updated {@link Account}.
   */
  AccountSettings updateAccountSettings(AccountSettings accountSettings);

  /**
   * Get the account settings for the specified {@code accountId}.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return The requested {@link Account}, if present.
   */
  Optional<AccountSettings> getAccountSettings(AccountId accountId);

  /**
   * Get the account settings for the specified {@code accountId}.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return The requested {@link Account}, if present.
   *
   * @throws AccountNotFoundProblem if no AccountSettings exist for the specified accountId.
   */
  default AccountSettings safeGetAccountSettings(AccountId accountId) {
    // TODO: Consider an AccountSettingsNotFoundException?
    return this.getAccountSettings(accountId)
      .orElseThrow(() -> new RuntimeException("No AccountSettings found for Id: " + accountId));
  }

  /**
   * Accessor for all accounts in this manager, as a {@code Stream}.
   */
  //Stream<Account> getAllAccounts();
}
