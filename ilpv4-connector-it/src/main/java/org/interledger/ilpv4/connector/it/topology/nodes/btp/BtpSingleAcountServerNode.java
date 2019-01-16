package org.interledger.ilpv4.connector.it.topology.nodes.btp;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import org.interledger.ilpv4.connector.it.topology.AbstractServerNode;

/**
 * A Node that simulates an ILPv4 Connector running a single-account BTP server connection.
 *
 * @deprecated Use PluginNode instead.
 */
@Deprecated
public class BtpSingleAcountServerNode extends AbstractServerNode<ConnectorServer> {

  //private static final String CONNECTOR_BEAN = "ilpConnector";

  /**
   * Required-args Constructor.
   */
  public BtpSingleAcountServerNode(final ConnectorServer server) {
    super(server, "ws", "localhost");
    //this.pluginAddress = Objects.requireNonNull(pluginAddress);
  }

  @Override
  public String toString() {
    return getILPv4Connector().getAddress().toString();
  }

  //  /**
  //   * The Spring server contains the content-object, so we look in there to find and return it.
  //   */
  //  public ILPv4Connector getIlpV4ConnectorNode() {
  //    return (ILPv4Connector) this.getServer().getContext().getBean(CONNECTOR_BEAN);
  //  }

  /**
   * This node contains a Spring Server, so to return its {@link ILPv4Connector} we need to inspect the
   * application-context.
   */
  public ILPv4Connector getILPv4Connector() {
    return this.getServer().getContext().getBean(ILPv4Connector.class);
  }
}
