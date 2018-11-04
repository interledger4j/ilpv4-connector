package org.interledger.ilpv4.connector.it;

import com.sappenin.ilpv4.IlpConnector;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.nodes.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.ServerNode;

import static org.hamcrest.CoreMatchers.is;
import static org.interledger.ilpv4.connector.it.graph.nodes.ConnectorNode.CONNECTOR_BEAN;
import static org.junit.Assert.assertThat;

public abstract class AbstractArchitectureTest {

  /**
   * Helper method to obtain an instance of {@link IlpConnector} from the graph, based upon its Interledger Address.
   *
   * @param interledgerAddress
   *
   * @return
   */
  protected IlpConnector getIlpConnectorFromGraph(final InterledgerAddress interledgerAddress) {
    final IlpConnector connector = (IlpConnector) ((ServerNode) getGraph().getNode(interledgerAddress.getValue()))
      .getServer().getContext().getBean(CONNECTOR_BEAN);
    assertThat(connector.getConnectorSettings().getIlpAddress(), is(interledgerAddress));

    return connector;
  }

  protected abstract Graph getGraph();

  protected static ConnectorNode toConnectorNode(final Graph graph, final InterledgerAddress nodeName) {
    return ((ConnectorNode) graph.getNode(nodeName.getValue()));
  }
}
