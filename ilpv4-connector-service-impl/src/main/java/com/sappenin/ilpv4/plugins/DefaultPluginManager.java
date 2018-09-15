package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.accounts.AccountManager;

/**
 * Meant to only be accessed by the {@link AccountManager}.
 */
// TODO: Remove if unused.
public class DefaultPluginManager {//implements PluginManager {

  //  private final Map<InterledgerAddress, Plugin> plugins;
  //
  //  public DefaultPluginManager() {
  //    this.plugins = Maps.newConcurrentMap();
  //  }
  //
  //  @Override
  //  public void setPlugin(final InterledgerAddress accountAddress, final Plugin plugin) {
  //    Objects.requireNonNull(accountAddress);
  //    Objects.requireNonNull(plugin);
  //    if (plugins.putIfAbsent(accountAddress, plugin) != null) {
  //      throw new RuntimeException(
  //        String.format("Plugin already exists with InterledgerAddress: %s", accountAddress)
  //      );
  //    }
  //  }
  //
  //  @Override
  //  public Optional<Plugin> getPlugin(final InterledgerAddress accountAddress) {
  //    Objects.requireNonNull(accountAddress);
  //    return Optional.ofNullable(this.plugins.get(accountAddress));
  //  }
}
