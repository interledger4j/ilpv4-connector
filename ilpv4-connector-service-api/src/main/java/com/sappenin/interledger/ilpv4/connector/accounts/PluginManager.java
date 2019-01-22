package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.Account;
import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;

/**
 * Defines how to operate on plugins outside the context of an {@link Account}.
 */
public interface PluginManager {

  /**
   * Creates an instance of {@link Plugin} using the supplied {@code accountId} and {@code pluginSettings}.
   *
   * @param accountId      The {@link AccountId} of the plugin to create.
   * @param pluginSettings The type of plugin to create.
   *
   * @return A newly constructed instance of {@link Plugin}.
   *
   * @throws RuntimeException if the plugin already exists.
   */
  Plugin<?> createPlugin(final AccountId accountId, final PluginSettings pluginSettings);

}
