package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginId;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.factories.PluginFactoryProvider;
import org.interledger.plugin.lpiv2.events.PluginConnectedEvent;
import org.interledger.plugin.lpiv2.events.PluginDisconnectedEvent;
import org.interledger.plugin.lpiv2.events.PluginErrorEvent;
import org.interledger.plugin.lpiv2.events.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A default implementation of {@link PluginManager} that stores all plugins in-memory.
 */
public class DefaultPluginManager implements PluginManager, PluginEventListener {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final EventBus eventBus;
  private final PluginFactoryProvider pluginFactoryProvider;

  /**
   * Required-args constructor.
   */
  public DefaultPluginManager(final EventBus eventBus, final PluginFactoryProvider pluginFactoryProvider) {
    this.eventBus = Objects.requireNonNull(eventBus);
    this.eventBus.register(this);
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

    // TODO: Refactor Plugin to require a pluginId. We shouldn't construct a plugin (client or server) if we don't
    //  know the Account, which is the PluginId.

    // Set the PluginId to match the AccountId...this way the Plugin can always use this value to represent the
    // accountId that a given plugin should use.
    ((AbstractPlugin) plugin).setPluginId(PluginId.of(accountId.value()));

    return plugin;
  }

  @Override
  // No need to @Subscribe
  public void onConnect(PluginConnectedEvent event) {
    // No-op.
  }

  @Override
  // No need to @Subscribe
  public void onDisconnect(PluginDisconnectedEvent event) {
    // No-op.
  }

  @Override
  @Subscribe
  public void onError(PluginErrorEvent event) {
    Objects.requireNonNull(event);
    logger.error("Plugin: {}; PluginError: {}", event.getPlugin(), event.getError());
  }
}
