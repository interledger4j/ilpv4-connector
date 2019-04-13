package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * An {@link AccountManager} that uses spring-data to abstract away the actual underlying datastore.
 */
public class SpringDataAccountManager implements AccountManager {




  /**
   * Create an {@link Account} using the supplied {@code accountSettings}. The account won't
   *
   * @param accountSettings
   */
  @Override
  public Account createAccount(AccountSettings accountSettings) {
    return null;
  }

  /**
   * Add an account to this manager using configured settings appropriate for the account. If the account already
   * exists, it is simply returned (i.e., no new account is constructed or connected)
   *
   * @param account
   */
  @Override
  public Account addAccount(Account account) throws RuntimeException {
    return null;
  }

  /**
   * Remove an account from this manager by its unique id.
   *
   * @param accountId
   *
   * @return The removed account, if any.
   */
  @Override
  public Optional<Account> removeAccount(AccountId accountId) {
    return Optional.empty();
  }

  /**
   * Returns the primary parent-account. A Connector can have multiple accounts of type `parent`, but only one can be
   * the primary account, e.g., for purposes of IL-DCP and other protocols that operate in conjunction with the primary
   * parent.
   */
  @Override
  public Optional<Account> getPrimaryParentAccount() {
    return Optional.empty();
  }

  /**
   * Get the account settings for the specified {@code accountId}.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return The requested {@link Account}, if present.
   */
  @Override
  public Optional<Account> getAccount(AccountId accountId) {
    return Optional.empty();
  }

  /**
   * Accessor for all accounts in this manager, as a {@code Stream}.
   */
  @Override
  public Stream<Account> getAllAccounts() {
    return null;
  }

  /**
   * Convert a child account into an address scoped underneath this connector. For example, given an input address,
   * append it to this connector's address to create a child address that this Connector can advertise as its own.
   *
   * @param accountId
   *
   * @return
   */
  @Override
  public InterledgerAddress toChildAddress(AccountId accountId) {
    return null;
  }
}
