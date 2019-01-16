package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.Account;
import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.core.InterledgerAddress;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>This manager contains all accounts this connector might use during its operations. Accounts are added to this
 * manager at startup (for preconfigured accounts, like a BTP client account) and are also be added to this manager by
 * server plugins whenever a new connection is initiated (e.g., a BTP Server plugin).</p>
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
   * Stop tracking the Account. This will remove this account from consideration for packet-switching, as well as
   * disable any Routing messages (if appropriate).
   *
   * @return The account that was untracked, if any.
   */
  // Optional<Account> stopAccountTracking(InterledgerAddress accountAddress) throws RuntimeException;

  /**
   * Add an account to this manager using configured settings appropriate for the account. If the account already
   * exists, it is simply returned (i.e., no new account is constructed or connected)
   */
  Account addAccount(Account account) throws RuntimeException;

  /**
   * Start tracking the Account. This will enable this account to participate in packet-switching, as well as Routing
   * (if appropriate).
   *
   * @return The account that was tracked.
   */
  // void startAccountTracking(InterledgerAddress accountAddress) throws RuntimeException;

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
