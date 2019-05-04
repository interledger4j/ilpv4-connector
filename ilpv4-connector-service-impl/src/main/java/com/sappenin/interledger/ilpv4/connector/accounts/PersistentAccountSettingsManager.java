package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.Account;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link AccountManager} that stores account-related information into a persistent datastore, such
 * as a RDBMS or NoSQL datastore.
 *
 * Note: This class should never know anything about routing.
 */
public class PersistentAccountSettingsManager {//implements AccountManager {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final AccountSettingsRepository accountSettingsRepository;
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
  public PersistentAccountSettingsManager(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountSettingsRepository accountSettingsRepository,
    final AccountIdResolver accountIdResolver,
    final AccountSettingsResolver accountSettingsResolver,
    final LinkManager linkManager,
    final EventBus eventBus
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.accountSettingsResolver = Objects.requireNonNull(accountSettingsResolver);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.eventBus = Objects.requireNonNull(eventBus);
    this.eventBus.register(this);
  }

  //  @Override
  //  public Account createAccount(final AccountSettings accountSettings) {
  //    Objects.requireNonNull(accountSettings);
  //
  //    // Create Data Link
  //    final LinkSettings linkSettings = LinkSettings.builder()
  //      .linkType(accountSettings.getLinkType())
  //      .customSettings(accountSettings.getCustomSettings())
  //      .build();
  //    final Link<?> link = linkManager.createLink(accountSettings.getAccountId(), linkSettings);
  //    // Register the Connector as a LinkEvent Listener...
  //    link.addLinkEventListener(this);
  //
  //    // Add the account, regardless of if the link is connected.
  //    final Account createdAccount = this.addAccount(
  //      Account.builder()
  //        //.id(accountId) // Found in AccountSettings.
  //        .accountSettings(accountSettings)
  //        .link(link)
  //        .build()
  //    );

  ////////////////////////////
  // Internal Tracking Account?
  ////////////////////////////

  //    // Create an internal Connector tracking account for this account. This is used by the BalanceTracker for
  //    // double-entry accounting purposes (i.e., BalanceTracker only ever transfers funds, so it needs two accounts in
  //    // order to "deposite" or "withdrawal" from any given external account.
  //    final AccountSettings trackingAccountSettings = AccountSettings.builder().from(accountSettings)
  //      .id(AccountId.of(accountSettings.getAccountId().value() + TRACKING_ACCOUNT_SUFFIX))
  //      .isReceiveRoutes(false)
  //      .isSendRoutes(false)
  //      .isInternal(true)
  //      .build();
  //
  //    // Add the account, regardless of if the link is connected.
  //    final Account trackingAccount = this.addAccount(
  //      Account.builder()
  //        .accountSettings(trackingAccountSettings)
  //        .link(link)
  //        .build()
  //    );

  ////////////////////////////
  // Connect the Link.
  ////////////////////////////
  //    try {
  //      // Try to connect, but only wait 15 seconds. Don't let one connection failure block the other links from
  //      // connecting.
  //      // TODO: No need to make this have a timeout. The Hystrix command will limit the amount of time this can take.
  //      link.connect().get(15, TimeUnit.SECONDS);
  //    } catch (Exception e) {
  //      if (accountSettings.isDynamic()) {
  //        // Remove the dynamic account...
  //        this.removeAccount(accountSettings.getAccountId());
  //        logger.error("Unable to connect Dynamic Link ({}): {}", linkSettings, e.getMessage());
  //      } else {
  //        // Preconfigured accounts should not be removed...
  //        logger.warn("Unable to connect Preconfigured Link({}): {}", linkSettings, e.getMessage());
  //      }
  //    }
  //
  //    return createdAccount;
  //}

//  /**
//   * Update an account's settings with new values.
//   *
//   * @param accountId       The unique identifier of the account to update.
//   * @param accountSettings The new settings to use for this account.
//   *
//   * @return The updated {@link Account}.
//   */
//  @Override
//  public Account updateAccountSettings(AccountId accountId, AccountSettings accountSettings) {
//    return null;
//  }
//
//  /**
//   * Add an account to this manager using configured settings appropriate for the account, as long as no pre-existing
//   * account already exists.
//   *
//   * @param account
//   *
//   * @throws RuntimeException if an account already exists with the same account id.
//   */
//  public Account addAccount(final Account account) {
//    Objects.requireNonNull(account);
//
//    // Set the primary parent-account, but only if it hasn't been set.
//    if (account.isParentAccount() && !primaryParentAccount.isPresent()) {
//      // Set the parent peer, if it exists.
//      this.setPrimaryParentAccount(Optional.of(account));
//    }
//
//    try {
//      if (accounts.putIfAbsent(account.getId(), account) != null) {
//        throw new RuntimeException(
//          String.format("Account may only be configured once in the AccountManager! AccountId: %s", account.getId())
//        );
//      }
//
//      return account;
//    } catch (Exception e) {
//      // If any exception is thrown, then remove the peer and any links...
//      this.removeAccount(account.getId());
//      throw new RuntimeException(e);
//    }
//  }
//
//  @Override
//  public Optional<Account> removeAccount(final AccountId accountId) {
//    Objects.requireNonNull(accountId);
//
//    this.getAccount(accountId).ifPresent(accountSettings -> {
//      // Disconnect the account...
//      // this.disconnectAccount(accountSettings.getAccountAddress());
//
//      if (accountSettings.getAccountSettings().getAccountRelationship() == AccountRelationship.PARENT) {
//        // Set the parent peer, if it exists.
//        this.setPrimaryParentAccount(Optional.empty());
//      }
//    });
//
//    return Optional.ofNullable(this.accounts.remove(accountId));
//  }
//
//  @Override
//  public Optional<Account> getPrimaryParentAccount() {
//    return this.primaryParentAccount;
//  }
//
//  /**
//   * Allows only a single thread to set a peer at a time, ensuring that only one will win.
//   */
//  private synchronized void setPrimaryParentAccount(final Optional<Account> account) {
//    Objects.requireNonNull(account);
//
//    if (account.isPresent()) {
//      if (this.primaryParentAccount.isPresent()) {
//        throw new RuntimeException("Only a single Primary Parent Account may be configured for a Connector!");
//      } else {
//        logger.info("PRIMARY PARENT ACCOUNT: `{}`", account.get().getId().value());
//        this.primaryParentAccount = account;
//      }
//    } else {
//      // Only log if present...
//      this.primaryParentAccount.ifPresent(
//        primaryParentAccount -> logger.warn("UN-SETTING PRIMARY PARENT ACCOUNT: `{}`", primaryParentAccount));
//      this.primaryParentAccount = Optional.empty();
//    }
//
//  }
//
//  @Override
//  public Optional<Account> getAccount(AccountId accountId) {
//    return Optional.ofNullable(this.accounts.get(accountId));
//  }
//
//  /**
//   * Create an {@link Account} using the supplied {@code accountSettings}. The account won't
//   *
//   * @param accountSettings
//   */
//  @Override
//  public AccountSettings createAccountSettings(AccountSettings accountSettings) {
//    return null;
//  }
//
//  /**
//   * Update an account's settings with new values.
//   *
//   * @param accountSettings The new settings to use for this account.
//   *
//   * @return The updated {@link Account}.
//   */
//  @Override
//  public AccountSettings updateAccountSettings(AccountSettings accountSettings) {
//    return null;
//  }
//
//  /**
//   * Get the account settings for the specified {@code accountId}.
//   *
//   * @param accountId The {@link AccountId} of the account to retrieve.
//   *
//   * @return The requested {@link Account}, if present.
//   */
//  @Override
//  public Optional<AccountSettings> getAccountSettings(AccountId accountId) {
//    return Optional.empty();
//  }
//
//  @Override
//  public Stream<Account> getAllAccounts() {
//    return accounts.values().stream();
//  }
//
//  @Override
//  public InterledgerAddress toChildAddress(final AccountId accountId) {
//    Objects.requireNonNull(accountId);
//    return this.connectorSettingsSupplier.get().getOperatorAddressSafe().with(accountId.value());
//  }
//
//  ////////////////////////
//  // Link Event Listener
//  ////////////////////////
//
//  /**
//   * When a multi-account link connects, we need to construct a new account in the AccountManager (if it doesn't
//   * exist).
//   */
//  @Override
//  @Subscribe
//  public void onConnect(final LinkConnectedEvent event) {
//    Objects.requireNonNull(event);
//
//    final AccountId accountId = accountIdResolver.resolveAccountId(event.getLink());
//    this.getAccount(accountId).orElseGet(() -> {
//      // If we get here, there is no account, so we need to add one to the AccountManager.
//      final AccountSettings accountSettings = this.accountSettingsResolver.resolveAccountSettings(event.getLink());
//      this.addAccount(
//        Account.builder()
//          //.id(accountId) // Found in AccountSettings.
//          .accountSettings(accountSettings)
//          .link(event.getLink())
//          .build()
//      );
//      // Ignore the result.
//      return null;
//    });
//  }
//
//  /**
//   * There are two types of accounts: pre-configured, and dynamic. Preconfigured accounts should never be removed from
//   * the account manager when they disconnect. However, in the case of dynamic accounts (i.e., accounts that get created
//   * dynamically in response to an incoming connection), these will take up memory and should not continue to be tracked
//   * if no connection is currently open.
//   */
//  @Override
//  @Subscribe
//  public void onDisconnect(final LinkDisconnectedEvent event) {
//    Objects.requireNonNull(event);
//
//    final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getLink());
//    this.getAccount(accountId).ifPresent(account -> {
//      if (account.getAccountSettings().isDynamic()) {
//        this.removeAccount(accountId);
//      }
//    });
//  }

}
