package org.interledger.connector.it.topology.nodes;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.topology.AbstractServerNode;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.core.InterledgerAddress;

/**
 * A Node that simulates an ILPv4 Connector.
 */
public class ConnectorServerNode extends AbstractServerNode<ConnectorServer> {

  private final String id;

  /**
   * Required-args Constructor.
   */
  public ConnectorServerNode(final String id, final ConnectorServer server) {
    super(server, "ws", "localhost");
    this.id = id;
  }

  @Override
  public String toString() {
    return getILPv4Connector().getConnectorSettings().operatorAddress()
      .map(InterledgerAddress::getValue)
      .orElse("n/a");
  }

  /**
   * This node contains a Spring Server, so to return its {@link ILPv4Connector} we need to inspect the
   * application-context.
   */
  public ILPv4Connector getILPv4Connector() {
    return this.getServer().getContext().getBean(ILPv4Connector.class);
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public void stop() {
    super.stop();
  }
}
