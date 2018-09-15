package com.sappenin.ilpv4.accounts;

import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.plugins.PluginManager;
import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.Plugin;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Manages IlpConnector accounts for a given Interledger address prefix.</p>
 *
 * <p>A connector might have multiple accounts with the same Counterparty, each of which may
 * have a different or identical currency code.</p>
 */
public interface AccountManager extends PluginManager {

  /**
   * Called just before this Peer Manager will be destroyed (i.e., disconnect all peers).
   */
  void shutdown();

  /**
   * Add a peer accountSettings to this manager.
   *
   * @throws RuntimeException if the accountSettings already exists.
   */
  void add(AccountSettings accountSettings);

  /**
   * Remove an account from this manager by its id.
   */
  void remove(InterledgerAddress interledgerAddress);

  /**
   * Returns the primary parent-account. A Connector can have multiple accounts of type `parent`, but only one can be
   * the primary account, e.g., for purposes of IL-DCP and other protocols that operate in conjunction with the primary
   * parent.
   */
  Optional<AccountSettings> getPrimaryParentAccountSettings();

  /**
   * Get the Ledger Layer2Plugin for the specified {@code ledgerPrefix}.
   *
   * @param interledgerAddress The {@link InterledgerAddress} of the account to retrieve.
   *
   * @return The requested {@link Account}, if present.
   */
  Optional<AccountSettings> getAccountSettings(InterledgerAddress interledgerAddress);

  /**
   * Gets the {@link Plugin} for the specified account address.
   */
  Plugin getOrCreatePlugin(InterledgerAddress interledgerAddress);

  /**
   * Accessor for all accounts in this manager, as a {@code Stream}.
   */
  Stream<AccountSettings> getAllAccountSettings();

  /**
   * The current balance of this account.
   *
   * @param accountAddress The {@link InterledgerAddress} of the account to retrieve a balance for.
   *
   * @return A {@link BigInteger} representing the balance of the account identified by {@code accountAddress}.
   */
  BigInteger getAccountBalance(InterledgerAddress accountAddress);

  /**
   * Given an input address, append it to this connector's address to create a child address that this Connector can
   * advertise as its own.
   *
   * @param interledgerAddress
   *
   * @return
   */
  InterledgerAddress constructChildAddress(InterledgerAddress interledgerAddress);
}
