package org.interledger.ilpv4.connector.it.graph;

import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.Plugin;

/**
 * A node in a graph which exposes one or more instances of {@link Plugin} that can be used to interact with the Node.
 */
public interface PluginNode<T> extends Node<T> {

  /**
   * Accessor for the plugin identified by {@code accountAddress}.
   */
  Plugin<?> getPlugin(InterledgerAddress accountAddress);

  /**
   * Allows for late-binding of the plugin, so that it can be created and set by an edge.
   */
  void setPlugin(final Plugin<?> clientPlugin);

}
