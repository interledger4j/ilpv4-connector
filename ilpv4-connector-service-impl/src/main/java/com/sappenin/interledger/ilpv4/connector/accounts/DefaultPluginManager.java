package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.plugins.IlpPluginFactory;
import org.interledger.core.InterledgerAddress;
import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.exceptions.PluginNotFoundException;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DefaultPluginManager implements AccountManager.PluginManager {

  private final Map<InterledgerAddress, Plugin> plugins;
  private final IlpPluginFactory ilpPluginFactory;

  /**
   * Required-args constructor.
   *
   * @param ilpPluginFactory
   */
  public DefaultPluginManager(final IlpPluginFactory ilpPluginFactory) {
    this.ilpPluginFactory = Objects.requireNonNull(ilpPluginFactory);
    this.plugins = Maps.newConcurrentMap();
  }

  /**
   * Associate the supplied {@code lpi2} to the supplied {@code accountAddress}.
   *
   * @param accountAddress
   * @param plugin
   */
  @Override
  public void setPlugin(InterledgerAddress accountAddress, Plugin plugin) {
    Objects.requireNonNull(accountAddress);
    Objects.requireNonNull(plugin);

    this.plugins.put(accountAddress, plugin);
  }

  /**
   * <p>Retrieve a {@link Plugin} for the supplied {@code accountAddress}.</p>
   *
   * <p>Note that this method returns one or zero plugins using an exact-match algorithm on the address because a
   * particular account can have only one plugin at a time.</p>
   *
   * @param peerAccountAddress The {@link InterledgerAddress} of the remote peer.
   *
   * @return An optinoally-present {@link Plugin}.
   */
  @Override
  public Optional<Plugin> getPlugin(InterledgerAddress peerAccountAddress) {
    return Optional.ofNullable(plugins.get(peerAccountAddress));
  }

  /**
   * @param peerAccountAddress
   *
   * @return
   *
   * @throws PluginNotFoundException if no plugin exists for this peer.
   */
  @Override
  public Plugin safeGetPlugin(InterledgerAddress peerAccountAddress) {
    return this.getPlugin(peerAccountAddress).orElseThrow(() -> new PluginNotFoundException(peerAccountAddress));
  }

  @Override
  public Plugin createPlugin(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    // TODO: This should throw if the plugin already exists!

    // Return the already constructed Plugin, or attempt to construct a new one...
    return this.getPlugin(accountSettings.getInterledgerAddress())
      .orElseGet(() -> {
          final Plugin<?> plugin = this.ilpPluginFactory.constructPlugin(accountSettings.getPluginSettings());
          // Add this plugin to the plugin-map, keyed by address.
          this.plugins.put(accountSettings.getInterledgerAddress(), plugin);
          return plugin;
        }
      );
  }
}
