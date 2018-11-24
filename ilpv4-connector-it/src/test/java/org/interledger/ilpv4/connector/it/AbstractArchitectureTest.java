package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.graph.Graph;

// TODO: Delete?
@Deprecated
public abstract class AbstractArchitectureTest {

  //  /**
  //   * Helper method to obtain an instance of {@link ILPv4Connector} from the graph, based upon its Interledger Address.
  //   *
  //   * @param interledgerAddress
  //   *
  //   * @return
  //   */
  //  protected ILPv4Connector getIlpConnectorFromGraph(final InterledgerAddress interledgerAddress) {
  //    final ILPv4Connector connector = (ILPv4Connector) ((ServerNode) getGraph().getNode(interledgerAddress.getValue()))
  //      .getServer().getContext().getBean(CONNECTOR_BEAN);
  //    assertThat(connector.getConnectorSettings().getIlpAddress(), is(interledgerAddress));
  //
  //    return connector;
  //  }

  protected abstract Graph getGraph();

  //  protected static IlpConnectorNode toConnectorNode(final Graph graph, final InterledgerAddress nodeName) {
  //    return ((IlpConnectorNode) graph.getNode(nodeName.getValue()));
  //  }
}
