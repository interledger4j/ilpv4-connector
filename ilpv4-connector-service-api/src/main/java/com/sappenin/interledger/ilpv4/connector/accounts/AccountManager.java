package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>This manager contains all accounts this connector might use during its operations. Accounts are added to this
 * manager at startup (for preconfigured accounts, like a BTP client account) and are also be added to this manager by
 * server links whenever a new connection is initiated (e.g., a BTP Server plugin).</p>
 *
 * <p>Even though an account has been added to this manager, is it not necessarily eligible to be used by the
 * Connector until it has been added to the `tracked accounts` collection. An account is only tracked when a plugin
 * connects successfully. When a plugin disconnects, the account is moved into the un-tracked accounts collection so
 * that it is no longer eligible to participate in Connector functionality (however, the account still exists in the
 * Manager).</p>
 *
 * <p>In this way, accounts must be added to the Account manager before they can be tracked (i.e., adding and
 * tracking are distinct operations).
 */
public interface AccountManager {

  /**
   * Create an {@link Account} using the supplied {@code accountSettings}. The account won't
   *
   * @param accountSettings
   */
  Account createAccount(final AccountSettings accountSettings);

  /**
   * Add an account to this manager using configured settings appropriate for the account. If the account already
   * exists, it is simply returned (i.e., no new account is constructed or connected)
   */
  Account addAccount(Account account) throws RuntimeException;

  /**
   * Remove an account from this manager by its unique id.
   *
   * @return The removed account, if any.
   */
  Optional<Account> removeAccount(AccountId accountId);

  /**
   * Returns the primary parent-account. A Connector can have multiple accounts of type `parent`, but only one can be
   * the primary account, e.g., for purposes of IL-DCP and other protocols that operate in conjunction with the primary
   * parent.
   */
  Optional<Account> getPrimaryParentAccount();

  /**
   * Get the account settings for the specified {@code accountId}.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return The requested {@link Account}, if present.
   */
  Optional<Account> getAccount(AccountId accountId);

  /**
   * Determines if the account represented by {@code accountId} is internal.
   *
   * @param accountId
   *
   * @return {@code true} or {@code false}, or {@link Optional#empty()} if the account doesn't exist.
   */
  default Optional<Boolean> isInternal(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    return this.getAccount(accountId)
      .map(Account::getAccountSettings)
      .map(AccountSettings::isInternal)
      .map(Optional::ofNullable)
      .orElse(Optional.empty());
  }

  /**
   * Determines if the account represented by {@code accountId} is internal.
   *
   * @param accountId
   *
   * @return {@code true} or {@code false}, or {@link Optional#empty()} if the account doesn't exist.
   */
  default Optional<Boolean> isNotInternal(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    return this.isInternal(accountId)
      .map(isInternal -> !isInternal);
  }

  /**
   * Get the account settings for the specified {@code accountId}.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return The requested {@link Account}, if present.
   */
  default Account safeGetAccount(AccountId accountId) {
    // TODO: Consider an AccountNotFoundException?
    return this.getAccount(accountId).orElseThrow(() -> new RuntimeException("No Account found for Id: " + accountId));
  }

  /**
   * Accessor for all accounts in this manager, as a {@code Stream}.
   */
  Stream<Account> getAllAccounts();

  /**
   * The current balance of this account.
   *
   * @param accountAddress The {@link InterledgerAddress} of the account to retrieve a balance for.
   *
   * @return A {@link BigInteger} representing the balance of the account identified by {@code accountAddress}.
   */
  //BigInteger getAccountBalance(InterledgerAddress accountAddress);

  /**
   * Convert a child account into an address scoped underneath this connector. For example, given an input address,
   * append it to this connector's address to create a child address that this Connector can advertise as its own.
   *
   * @param accountId
   *
   * @return
   */
  InterledgerAddress toChildAddress(AccountId accountId);

}
