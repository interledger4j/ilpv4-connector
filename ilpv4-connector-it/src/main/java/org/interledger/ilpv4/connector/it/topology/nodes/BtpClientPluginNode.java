package org.interledger.ilpv4.connector.it.topology.nodes;

import org.interledger.ilpv4.connector.it.topology.PluginNode;
import org.interledger.plugin.lpiv2.btp2.spring.BtpClientPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.BtpClientPluginSettings;

/**
 * An extension of {@link PluginNode} that is more narrowly typed.
 */
public class BtpClientPluginNode extends PluginNode<BtpClientPluginSettings, BtpClientPlugin> {

  public BtpClientPluginNode(final BtpClientPlugin btpClientPlugin) {
    super(btpClientPlugin);
  }

}
