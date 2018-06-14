package com.sappenin.ilpv4.accounts;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.plugins.MockPlugin;
import com.sappenin.ilpv4.settings.ConnectorSettings;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The default implementation of {@link AccountManager}.
 */
public class DefaultAccountManager implements AccountManager {

  private final ConnectorSettings connectorSettings;
  private final Map<String, Account> accounts = Maps.newConcurrentMap();

  public DefaultAccountManager(final ConnectorSettings connectorSettings) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
  }

  @Override
  public void add(final Account account) {
    Objects.requireNonNull(account);
    if (!accounts.replace(account.getInterledgerAddress(), null, account)) {
      throw new RuntimeException(
        String.format("Account already exists with InterledgerAddress: %s", account.getInterledgerAddress())
      );
    }
  }

  @Override
  public void remove(String interledgerAddress) {
    this.accounts.remove(interledgerAddress);
  }

  @Override
  public Optional<Account> getAccount(String interledgerAddress) {
    return Optional.ofNullable(this.accounts.get(interledgerAddress));
  }

  @Override
  public Stream<Account> stream() {
    return accounts.values().stream();
  }

  @Override
  public Plugin getPlugin(String interledgerAddress) {
    return this.getAccount(interledgerAddress)
      .map(account -> {
        switch (account.getPluginType()) {
          case MOCK: {
            return new MockPlugin(connectorSettings, interledgerAddress);
          }
          case BTP: {

          }
          default: {
            throw new RuntimeException(String.format("Unsupported PluginType: %s", account.getPluginType()));
          }
        }
      })
      .orElseThrow(() -> new RuntimeException("Unable to obtain plugin (No Accounts Configured)"));
  }
}
