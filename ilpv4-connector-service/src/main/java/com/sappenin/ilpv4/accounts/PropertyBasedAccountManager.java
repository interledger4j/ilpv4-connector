package com.sappenin.ilpv4.accounts;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.model.AccountId;
import com.sappenin.ilpv4.model.AccountRelationship;
import com.sappenin.ilpv4.plugins.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An implementation of {@link AccountManager} that loads accounts from a properties file.
 */
public class PropertyBasedAccountManager implements AccountManager {

  private final Set<Account> accounts;
  private final Map<AccountId, Plugin> plugins;
  private Optional<Account> parentAccount = Optional.empty();

  public PropertyBasedAccountManager() {
    this.accounts = Sets.newConcurrentHashSet();
    this.plugins = Maps.newConcurrentMap();
  }

  @Override
  public Optional<Account> getParentAccount() {
    return this.parentAccount;
  }

  @Override
  public boolean add(Account account) {
    Objects.requireNonNull(account);

    if (account.getAccountOptions().getRelationship() == AccountRelationship.PARENT) {
//    // Set the parent account, if it exists.
//    this.parentAccount = Objects.requireNonNull(connectorSpringProperties)
//      .getAccounts().stream()
//      .map(AccountProperties::toAccount)
//      .filter(account -> account.getAccountOptions().getRelationship()
//        == AccountRelationship.PARENT)
//      .findFirst()
//      .map(Optional::of)
//      .orElseGet(Optional::empty);
      return false;
    } else {
      return accounts.add(account);
    }
  }

  @Override
  public void remove(final AccountId accountId) {
    this.accounts.remove(accountId);
  }

  @Override
  public Optional<Account> getAccount(final AccountId accountId) {
    return Optional.empty();
  }

  @Override
  public Stream<Account> stream() {
    return accounts.stream();
  }

  @Override
  public Plugin getPlugin(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    return Optional.ofNullable(this.plugins.get(accountId))
      .orElseThrow(() -> new RuntimeException(String.format("No Plugin found for accountId: %s", accountId)));
  }
}
