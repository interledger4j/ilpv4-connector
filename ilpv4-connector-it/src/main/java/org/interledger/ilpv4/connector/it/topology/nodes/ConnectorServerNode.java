package org.interledger.ilpv4.connector.it.topology.nodes;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import org.interledger.ilpv4.connector.it.topology.AbstractServerNode;

/**
 * A Node that simulates an ILPv4 Connector.
 */
public class ConnectorServerNode extends AbstractServerNode<ConnectorServer> {

  /**
   * Required-args Constructor.
   */
  public ConnectorServerNode(final ConnectorServer server) {
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
