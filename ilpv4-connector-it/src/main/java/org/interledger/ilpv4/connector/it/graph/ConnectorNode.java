package org.interledger.ilpv4.connector.it.graph;

import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.server.support.Server;

/**
 * An implementation of {@link Node} that contains a {@link Server} running an ILP Connectors.
 */
public class ConnectorNode extends ServerNode implements Node {

  //private static final Logger logger = LoggerFactory.getLogger(PeeringEdge.class);

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
    return ((IlpConnector) this.getServer().getContext().getBean("connector")).getConnectorSettings().getIlpAddress()
      .toString();
  }
}
