package com.sappenin.ilpv4.accounts;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.plugins.MockChildPlugin;
import com.sappenin.ilpv4.plugins.PluginManager;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The default implementation of {@link AccountManager}.
 */
public class DefaultAccountManager implements AccountManager {

  private final ConnectorSettings connectorSettings;
  private final PluginManager pluginManager;
  private final Map<InterledgerAddress, Account> accounts = Maps.newConcurrentMap();

  public DefaultAccountManager(final ConnectorSettings connectorSettings, final PluginManager pluginManager) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.pluginManager = Objects.requireNonNull(pluginManager);
  }

  @Override
  public void add(final Account account) {
    Objects.requireNonNull(account);
    if (accounts.putIfAbsent(account.getInterledgerAddress(), account) != null) {
      throw new RuntimeException(
        String.format("Account already exists with InterledgerAddress: %s", account.getInterledgerAddress())
      );
    }
  }

  @Override
  public void remove(InterledgerAddress interledgerAddress) {
    this.accounts.remove(interledgerAddress);
  }

  @Override
  public Optional<Account> getAccount(InterledgerAddress interledgerAddress) {
    return Optional.ofNullable(this.accounts.get(interledgerAddress));
  }

  @Override
  public Stream<Account> stream() {
    return accounts.values().stream();
  }

  @Override
  public Plugin getPlugin(final InterledgerAddress accountAddress) {
    Objects.requireNonNull(accountAddress);

    return this.getAccount(accountAddress)
      .map(account -> {
        switch (account.getPluginType()) {
          case MOCK: {
            final Plugin plugin = new MockChildPlugin(connectorSettings, accountAddress);
            this.pluginManager.setPlugin(accountAddress, plugin);
            return plugin;
          }
          case BTP: {
            throw new RuntimeException("Not yet implemented!");
          }
          default: {
            throw new RuntimeException(String.format("Unsupported PluginType: %s", account.getPluginType()));
          }
        }
      })
      .orElseThrow(
        () -> new RuntimeException(String.format("Unable to obtain plugin for Account %s!", accountAddress))
      );
  }
}
