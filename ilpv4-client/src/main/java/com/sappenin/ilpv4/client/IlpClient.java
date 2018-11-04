package com.sappenin.ilpv4.client;

import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;

/**
 * <p>A client for communicating with an ILPv4 Service Provider, such as a Connector.</p>
 *
 * <p>This interface extends {@link Plugin} in order to share implementations with servers that might also act as a
 * Client when talking to remote servers.</p>
 */
public interface IlpClient<T extends PluginSettings> extends Plugin<T> {

  Plugin<T> getPluginDelegate();

  /**
   * Setter for DI.
   *
   * @param pluginDelegate An instance of {@link Plugin} to delegate all calls to for this client.
   */
  void setPluginDelegate(final Plugin<T> pluginDelegate);
}
