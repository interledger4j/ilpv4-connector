package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.connector.link.events.LinkDisconnectedEvent;
import org.interledger.connector.link.events.LinkEventListener;
import org.interledger.core.InterledgerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker.TRACKING_ACCOUNT_SUFFIX;

/**
 * An implementation of {@link AccountManager} that supports multiple accounts, each with its own link, storing all data
 * in memory.
 *
 * WARNING: This Account manager should never know anything about routing.
 */
public class InMemoryAccountManager implements AccountManager, LinkEventListener {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final Map<AccountId, Account> accounts = Maps.newConcurrentMap();

  private final AccountIdResolver accountIdResolver;
  private final AccountSettingsResolver accountSettingsResolver;
  private final LinkManager linkManager;
  private final EventBus eventBus;

  // A Connector can have multiple accounts of type `parent`, but only one can be the primary account, e.g., for
  // purposes of IL-DCP and other protocols.
  private Optional<Account> primaryParentAccount = Optional.empty();

  /**
   * Required-args Constructor.
   */
  public InMemoryAccountManager(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountIdResolver accountIdResolver,
    final AccountSettingsResolver accountSettingsResolver,
    final LinkManager linkManager,
    final EventBus eventBus
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.accountSettingsResolver = Objects.requireNonNull(accountSettingsResolver);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.eventBus = Objects.requireNonNull(eventBus);
    this.eventBus.register(this);
  }

  @Override
  public void createAccount(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    // Create Data Link
    final LinkSettings linkSettings = LinkSettings.builder()
      .linkType(accountSettings.getLinkType())
      .customSettings(accountSettings.getCustomSettings())
      .operatorAddress(connectorSettingsSupplier.get().getOperatorAddress())
      .build();
    final Link<?> link = linkManager.createLink(accountSettings.getId(), linkSettings);
    // Register the Connector as a LinkEvent Listener...
    link.addLinkEventListener(this);

    // Add the account, regardless of if the link is connected.
    this.addAccount(
      Account.builder()
        //.id(accountId) // Found in AccountSettings.
        .accountSettings(accountSettings)
        .link(link)
        .build()
    );

    ////////////////////////////
    // Internal Tracking Account
    ////////////////////////////

    // Create an internal Connector tracking account for this account. This is used by the BalanceTracker for
    // double-entry accounting purposes (i.e., BalanceTracker only ever transfers funds, so it needs two accounts in
    // order to "deposite" or "withdrawal" from any given external account.
    final AccountSettings trackingAccountSettings = AccountSettings.builder().from(accountSettings)
      .id(AccountId.of(accountSettings.getId().value() + TRACKING_ACCOUNT_SUFFIX))
      .isReceiveRoutes(false)
      .isSendRoutes(false)
      .isInternal(true)
      .build();

    // Add the account, regardless of if the link is connected.
    this.addAccount(
      Account.builder()
        .accountSettings(trackingAccountSettings)
        .link(link)
        .build()
    );

    ////////////////////////////
    // Connect the Link.
    ////////////////////////////
    try {
      // Try to connect, but only wait 15 seconds. Don't let one connection failure block the other links from
      // connecting.
      link.connect().get(15, TimeUnit.SECONDS);
    } catch (Exception e) {
      if (accountSettings.isDynamic()) {
        // Remove the dynamic account...
        this.removeAccount(accountSettings.getId());
        logger.error("Unable to connect Dynamic Link ({}): {}", linkSettings, e.getMessage());
      } else {
        // Preconfigured accounts should not be removed...
        logger.warn("Unable to connect Preconfigured Link({}): {}", linkSettings, e.getMessage());
      }
    }
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
      // If any exception is thrown, then removeEntry the peer and any links...
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
    throw new RuntimeException("AccountBalance Tracker not yet implemented!");
  }

  @Override
  public InterledgerAddress toChildAddress(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return this.connectorSettingsSupplier.get().getOperatorAddress().with(accountId.value());
  }

  ////////////////////////
  // Link Event Listener
  ////////////////////////

  /**
   * When a multi-account link connects, we need to construct a new account in the AccountManager (if it doesn't
   * exist).
   */
  @Override
  @Subscribe
  public void onConnect(final LinkConnectedEvent event) {
    Objects.requireNonNull(event);

    final AccountId accountId = accountIdResolver.resolveAccountId(event.getLink());
    this.getAccount(accountId).orElseGet(() -> {
      // If we get here, there is no account, so we need to add one to the AccountManager.
      final AccountSettings accountSettings = this.accountSettingsResolver.resolveAccountSettings(event.getLink());
      this.addAccount(
        Account.builder()
          //.id(accountId) // Found in AccountSettings.
          .accountSettings(accountSettings)
          .link(event.getLink())
          .build()
      );
      // Ignore the result.
      return null;
    });
  }

  /**
   * There are two types of accounts: pre-configured, and dynamic. Preconfigured accounts should never be removed from
   * the account manager when they disconnect. However, in the case of dynamic accounts (i.e., accounts that get created
   * dynamically in response to an incoming connection), these will take up memory and should not continue to be tracked
   * if no connection is currently open.
   */
  @Override
  @Subscribe
  public void onDisconnect(final LinkDisconnectedEvent event) {
    Objects.requireNonNull(event);

    final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getLink());
    this.getAccount(accountId).ifPresent(account -> {
      if (account.getAccountSettings().isDynamic()) {
        this.removeAccount(accountId);
      }
    });
  }

}
