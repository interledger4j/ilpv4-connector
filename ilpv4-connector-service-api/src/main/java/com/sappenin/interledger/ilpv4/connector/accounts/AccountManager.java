package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.exceptions.PluginNotFoundException;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Manages accounts for a given Interledger address prefix.</p>
 *
 * <p>A node might have multiple accounts with the same counterparty, each of which may have a different or
 * identical currency code.</p>
 *
 * <p>This interface is currently structured as a service, such that there is no Java object called an
 * <tt>Account</tt>. Instead, this service is used to retrieve or operate on Account-related information, such as a
 * balance. In this way, it's possible to operate on an Account via this interface, or get the settings for an account.
 * Otherwise, there is no formal <tt>account</tt> primitive or object.</p>
 */
public interface AccountManager {

  /**
   * Called just before this Peer Manager will be destroyed (i.e., disconnect all peers).
   */
  void shutdown();

  /**
   * Add a peer accountSettings to this manager.
   *
   * @throws RuntimeException if the accountSettings already exists.
   */
  Plugin add(AccountSettings accountSettings);

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
   * Get the account settings for the specified {@code interledgerAddress}.
   *
   * @param interledgerAddress The {@link InterledgerAddress} of the account to retrieve.
   *
   * @return The requested {@link AccountSettings}, if present.
   */
  Optional<AccountSettings> getAccountSettings(InterledgerAddress interledgerAddress);

  /**
   * Accessor for the manager that controls all plugins for any accounts defined in this manager.
   *
   * @return
   */
  PluginManager getPluginManager();

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
   * Convert a child account into an address scoped underneath this connector. For example, given an input address,
   * append it to this connector's address to create a child address that this Connector can advertise as its own.
   *
   * @param interledgerAddress
   *
   * @return
   */
  InterledgerAddress toChildAddress(InterledgerAddress interledgerAddress);

  /**
   * Defines how to operate on plugins associated with a particular {@link AccountManager}.
   */
  interface PluginManager {

    /**
     * Associate the supplied {@code lpi2} to the supplied {@code accountAddress}.
     *
     * @param accountAddress
     * @param plugin
     */
    void setPlugin(InterledgerAddress accountAddress, Plugin plugin);

    /**
     * <p>Retrieve a {@link Plugin} for the supplied {@code accountAddress}.</p>
     *
     * <p>Note that this method returns one or zero plugins using an exact-match algorithm on the address because a
     * particular account can have only one plugin at a time.</p>
     *
     * @param peerAccountAddress The {@link InterledgerAddress} of the remote peer.
     *
     * @return An optinoally-present {@link Plugin}.
     */
    Optional<Plugin> getPlugin(InterledgerAddress peerAccountAddress);

    /**
     * @param peerAccountAddress
     *
     * @return
     *
     * @throws PluginNotFoundException if no plugin exists for this peer.
     */
    Plugin safeGetPlugin(InterledgerAddress peerAccountAddress);

    Plugin createPlugin(final AccountSettings accountSettings);
  }
}
