package com.sappenin.ilpv4.accounts;

import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.model.AccountId;
import com.sappenin.ilpv4.model.Plugin;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Manages IlpConnector accounts for a given Interledger address prefix.</p>
 *
 * <p>A connector might have multiple accounts with the same Counterparty, each of which may
 * have a different or identical currency code.</p>
 */
public interface AccountManager {

  /**
   * An optionlly-present parent account. //TODO: Add pointer to an overview of parent/child/peer relationships.
   */
  Optional<Account> getParentAccount();

  /**
   * Add a peer account to this manager.
   */
  boolean add(Account account);

  /**
   * Remove an account from this manager by its id.
   */
  void remove(AccountId accountId);

  /**
   * Get the Ledger Layer2Plugin for the specified {@code ledgerPrefix}.
   *
   * @param accountId The {@link AccountId} of the account to retrieve. ¬   * @return The requested {@link Account}, if
   *                  present.
   */
  Optional<Account> getAccount(AccountId accountId);

  /**
   * Creates a {@code Stream} of Accounts.
   */
  Stream<Account> stream();

  /**
   * Gets the {@link Plugin} for the specified account id.
   */
  Plugin getPlugin(AccountId accountId);

}
