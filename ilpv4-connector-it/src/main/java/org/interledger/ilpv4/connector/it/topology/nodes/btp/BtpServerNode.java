package org.interledger.ilpv4.connector.it.topology.nodes.btp;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import org.interledger.ilpv4.connector.it.topology.AbstractServerNode;

/**
 * A Node that simulates an ILPv4 Connector running a BTP-server connection.
 */
public class BtpServerNode extends AbstractServerNode<ConnectorServer> {

  /**
   * Required-args Constructor.
   */
  public BtpServerNode(final ConnectorServer server) {
    super(server, "ws", "localhost");
  }

  @Override
  public String toString() {
    return getILPv4Connector().getNodeIlpAddress().toString();
  }

  /**
   * This node contains a Spring Server, so to return its {@link ILPv4Connector} we need to inspect the
   * application-context.
   */
  public ILPv4Connector getILPv4Connector() {
    return this.getServer().getContext().getBean(ILPv4Connector.class);
  }
}
