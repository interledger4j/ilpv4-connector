package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginId;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.factories.PluginFactoryProvider;

import java.util.Objects;

/**
 * A default implementation of {@link PluginManager} that stores all plugins in-memory.
 */
public class DefaultPluginManager implements PluginManager {

  private final PluginFactoryProvider pluginFactoryProvider;

  /**
   * Required-args constructor.
   */
  public DefaultPluginManager(final PluginFactoryProvider pluginFactoryProvider) {
    this.pluginFactoryProvider = Objects.requireNonNull(pluginFactoryProvider);
  }

  public Plugin<?> createPlugin(final AccountId accountId, final PluginSettings pluginSettings) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(pluginSettings);

    //Use the first pluginFactory that supports the pluginType...
    final Plugin<?> plugin = this.pluginFactoryProvider.getPluginFactory(pluginSettings.getPluginType())
      .map(pluginFactory -> pluginFactory.constructPlugin(pluginSettings))
      .orElseThrow(() -> new RuntimeException(
        String.format("No registered PluginFactory supports: %s", pluginSettings.getPluginType()))
      );

    // Set the PluginId to match the AccountId...
    ((AbstractPlugin) plugin).setPluginId(PluginId.of(accountId.value()));

    return plugin;
  }
}
