package com.sappenin.ilpv4.plugins;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.model.InterledgerAddress;
import com.sappenin.ilpv4.model.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DefaultPluginManager implements PluginManager {

  private final Map<InterledgerAddress, Plugin> plugins;

  public DefaultPluginManager() {
    this.plugins = Maps.newConcurrentMap();
  }

  @Override
  public void setPlugin(final InterledgerAddress accountAddress, final Plugin plugin) {
    Objects.requireNonNull(accountAddress);
    Objects.requireNonNull(plugin);
    if (!plugins.replace(accountAddress, null, plugin)) {
      throw new RuntimeException(
        String.format("Plugin already exists with InterledgerAddress: %s", accountAddress)
      );
    }
  }

  @Override
  public Optional<Plugin> getPlugin(final InterledgerAddress accountAddress) {
    Objects.requireNonNull(accountAddress);
    return Optional.ofNullable(this.plugins.get(accountAddress));
  }
}
