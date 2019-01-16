package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.Account;
import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.settings.AccountRelationship;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An implementation of {@link AccountManager} that supports multiple accounts, each with its own plugin.
 *
 * WARNING: This Account manager should never know anything about routing.
 */
public class DefaultAccountManager implements AccountManager {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettings;

  private final Map<AccountId, Account> accounts = Maps.newConcurrentMap();

  // A Connector can have multiple accounts of type `parent`, but only one can be the primary account, e.g., for
  // purposes of IL-DCP and other protocols.
  private Optional<Account> primaryParentAccount = Optional.empty();

  /**
   * Required-args Constructor.
   */
  public DefaultAccountManager(final Supplier<ConnectorSettings> connectorSettings) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
  }

  /**
   * Add an account to this manager using configured settings appropriate for the account, as long as no pre-existing
   * account already exists.
   *
   * @param account
   *
   * @throws RuntimeException if an account already exists with the same account id.
   */
  public Account addAccount(final Account account) {
    Objects.requireNonNull(account);

    // Set the primary parent-account, but only if it hasn't been set.
    if (account.isParentAccount() && !primaryParentAccount.isPresent()) {
      // Set the parent peer, if it exists.
      this.setPrimaryParentAccount(account);
    }

    try {
      if (accounts.putIfAbsent(account.getId(), account) != null) {
        throw new RuntimeException(
          String.format("Account may only be configured once in the AccountManager! AccountId: %s", account.getId())
        );
      }

      return account;
    } catch (Exception e) {
      // If any exception is thrown, then removeEntry the peer and any plugins...
      this.removeAccount(account.getId());
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Account> removeAccount(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    this.getAccount(accountId).ifPresent(accountSettings -> {
      // Disconnect the account...
      //this.disconnectAccount(accountSettings.getAccountAddress());

      if (accountSettings.getAccountSettings().getRelationship() == AccountRelationship.PARENT) {
        // Set the parent peer, if it exists.
        this.unsetPrimaryParentAccountSettings();
      }
    });

    return Optional.ofNullable(this.accounts.remove(accountId));
  }

  @Override
  public Optional<Account> getPrimaryParentAccount() {
    return this.primaryParentAccount;
  }

  /**
   * Allows only a single thread to set a peer at a time, ensuring that only one will win.
   */
  private synchronized void setPrimaryParentAccount(final Account account) {
    Objects.requireNonNull(account);

    if (this.primaryParentAccount.isPresent()) {
      throw new RuntimeException("Only a single Primary Parent Account may be configured for a Connector!");
    }

    logger.info("Primary Parent Account: {}", account.getId());
    this.primaryParentAccount = Optional.of(account);
  }

  /**
   * Allows only a single thread to unset a peer at a time, ensuring that only one will win.
   */
  private synchronized void unsetPrimaryParentAccountSettings() {
    this.primaryParentAccount = Optional.empty();
  }

  @Override
  public Optional<Account> getAccount(AccountId accountId) {
    return Optional.ofNullable(this.accounts.get(accountId));
  }

  @Override
  public Stream<Account> getAllAccounts() {
    return accounts.values().stream();
  }

  public BigInteger getAccountBalance(InterledgerAddress accountAddress) {
    throw new RuntimeException("Balance Tracker not yet implemented!");
  }

  @Override
  public InterledgerAddress toChildAddress(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return this.connectorSettings.get().getOperatorAddress().with(accountId.value());
  }
}
