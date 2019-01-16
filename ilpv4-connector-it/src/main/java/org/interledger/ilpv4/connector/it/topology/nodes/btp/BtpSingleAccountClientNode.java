package org.interledger.ilpv4.connector.it.topology.nodes.btp;

import org.interledger.ilpv4.connector.it.topology.AbstractClientNode;
import org.interledger.plugin.lpiv2.btp2.spring.BtpClientPlugin;

/**
 * @deprecated No longer in-use. Use a BtpPluginNode instead.
 */
@Deprecated
public class BtpSingleAccountClientNode extends AbstractClientNode<BtpClientPlugin> {

  public BtpSingleAccountClientNode(final BtpClientPlugin clientPlugin) {
    super(clientPlugin);
  }

  @Override
  public void start() {
    getContentObject().connect().join();
  }

  @Override
  public void stop() {
    getContentObject().disconnect().join();
  }
}
