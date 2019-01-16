package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.plugin.lpiv2.LoopbackPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.btp2.spring.AbstractBtpPlugin;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Default implementation of {@link AccountSettingsResolver} that looks in the connector config to find an {@link
 * AccountSettings} object. If none is found, it returns a default account settings.
 *
 * TODO: Use Java SPI here so custom resolvers can be added.
 */
public class DefaultAccountIdResolver implements AccountIdResolver {

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  public DefaultAccountIdResolver(final Supplier<ConnectorSettings> connectorSettingsSupplier) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
  }

  @Override
  public AccountId resolveAccountId(Plugin<?> plugin) {

    if (plugin instanceof LoopbackPlugin) {
      // Connected Btp Plugins will have a BTP Session that can be used to get the accountId.
      final LoopbackPlugin loopbackPlugin = (LoopbackPlugin) plugin;
      return AccountId.of(loopbackPlugin.getPluginId().get().value());
    }
    if (plugin instanceof AbstractBtpPlugin) {
      // Connected Btp Plugins will have a BTP Session that can be used to get the accountId.
      final AbstractBtpPlugin abstractBtpPlugin = (AbstractBtpPlugin) plugin;

      return abstractBtpPlugin.getBtpSessionCredentials()
        .getAuthUsername()
        .map(AccountId::of)
        .orElseGet(() -> {
          // No AuthUserName, so get the AuthToken and hash it.
          //Route.HMAC(abstractBtpPlugin.getBtpSessionCredentials().getAuthToken());
          throw new RuntimeException("Not yet implemented!");
        });
    } else {
      throw new RuntimeException("Unsupported Plugin Class: " + plugin.getClass());
    }
  }
}
