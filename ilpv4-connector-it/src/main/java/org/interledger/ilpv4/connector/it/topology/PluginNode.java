package org.interledger.ilpv4.connector.it.topology;

import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;

/**
 * A node in a topology which exposes an instance of {@link Plugin} that can be used to interact with the Node.
 */
public class PluginNode<PS extends PluginSettings, P extends Plugin<PS>> extends AbstractNode<P> implements Node<P> {

  public PluginNode(final P contentObject) {
    super(contentObject);
  }

  /**
   * Start this node. Sometimes a node is started when the topology starts-up, but not always (e.g., a delayed start).
   */
  @Override
  public void start() {
    getContentObject().connect().join();
  }

  /**
   * Stop this node.
   */
  @Override
  public void stop() {
    getContentObject().disconnect().join();
  }


}
