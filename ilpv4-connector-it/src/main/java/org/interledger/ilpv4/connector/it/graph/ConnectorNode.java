package org.interledger.ilpv4.connector.it.graph;

import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.server.ConnectorServer;
import com.sappenin.ilpv4.server.support.Server;

/**
 * An implementation of {@link Node} that contains a {@link Server} running an ILP Connectors.
 */
public class ConnectorNode extends ServerNode implements Node {

  public static final String CONNECTOR_BEAN = "ilpConnector";

  /**
   * Required-args Constructor.
   *
   * @param server
   */
  public ConnectorNode(final Server server) {
    super(server);
  }

  @Override
  public String toString() {
    return getIlpConnector().getConnectorSettings().getIlpAddress().toString();
  }

  public IlpConnector getIlpConnector() {
    return ((IlpConnector) this.getServer().getContext().getBean(CONNECTOR_BEAN));
  }

  public ConnectorServer getServer() {
    return (ConnectorServer) super.getServer();
  }

}
