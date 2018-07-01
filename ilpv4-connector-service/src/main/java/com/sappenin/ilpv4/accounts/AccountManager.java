package com.sappenin.ilpv4.accounts;

import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.model.Plugin;
import org.interledger.core.InterledgerAddress;

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
   * Add a peer account to this manager.
   *
   * @throws RuntimeException if the account already exists.
   */
  void add(Account account);

  /**
   * Remove an account from this manager by its id.
   */
  void remove(InterledgerAddress interledgerAddress);

  /**
   * Get the Ledger Layer2Plugin for the specified {@code ledgerPrefix}.
   *
   * @param interledgerAddress The {@link InterledgerAddress} of the account to retrieve.
   *
   * @return The requested {@link Account}, if present.
   */
  Optional<Account> getAccount(InterledgerAddress interledgerAddress);

  /**
   * Creates a {@code Stream} of Accounts.
   */
  Stream<Account> stream();

  /**
   * Gets the {@link Plugin} for the specified account address.
   */
  Plugin getPlugin(InterledgerAddress interledgerAddress);

}
