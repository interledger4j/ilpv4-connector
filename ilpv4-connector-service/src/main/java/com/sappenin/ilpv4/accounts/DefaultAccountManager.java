package com.sappenin.ilpv4.accounts;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.model.PluginType;
import com.sappenin.ilpv4.plugins.MockChildPlugin;
import com.sappenin.ilpv4.plugins.PluginManager;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;

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

    // Add a plugin for this account based upon its settings...
    final Plugin plugin = this.constructPlugin(account.getInterledgerAddress(), account.getPluginType());
    this.pluginManager.setPlugin(account.getInterledgerAddress(), plugin);
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
      .map(Account::getPluginType)
      .map(pluginType -> {
        switch (pluginType) {
          case MOCK: {
            return this.pluginManager.getPlugin(accountAddress)
              // Return null to fall-through to an F02_UNREACHABLE (below)
              .orElse(null);
          }
          case BTP: {
            throw new RuntimeException("Not yet implemented!");
          }
          default: {
            throw new RuntimeException(String.format("Unsupported PluginType: %s", pluginType));
          }
        }
      })
      .orElseThrow(() -> new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(this.connectorSettings.getIlpAddress())
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(String.format("Tried to get a Plugin for non-existent account: %s", accountAddress))
          .build())
      );
  }

  @VisibleForTesting
  protected Plugin constructPlugin(final InterledgerAddress accountAddress, final PluginType pluginType) {

    Objects.requireNonNull(accountAddress);
    Objects.requireNonNull(pluginType);

    switch (pluginType) {
      case MOCK: {
        return new MockChildPlugin(connectorSettings, accountAddress);
      }
      case BTP: {
        throw new RuntimeException("Not yet implemented!");
      }
      default: {
        throw new RuntimeException(String.format("Unsupported PluginType: %s", pluginType));
      }
    }
  }
}
