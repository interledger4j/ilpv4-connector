package org.interledger.ilpv4.connector.it.topologies;

import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.model.settings.ImmutableConnectorSettings;
import com.sappenin.ilpv4.server.support.Server;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.ServerNode;

public class TopologyUtils {

  static ConnectorSettings defaultConnectorSettings(final InterledgerAddress interledgerAddress) {
    return ImmutableConnectorSettings.builder()
      .ilpAddress(interledgerAddress)
      //.secret("secret")
      .build();
  }

  static Server toServerNode(final Graph graph, final InterledgerAddress nodeName) {
    return ((ServerNode) graph.getNode(nodeName.getValue())).getServer();
  }

  static ConnectorNode toConnectorNode(final Graph graph, final InterledgerAddress nodeName) {
    return ((ConnectorNode) graph.getNode(nodeName.getValue()));
  }

}
