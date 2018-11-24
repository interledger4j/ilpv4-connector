package org.interledger.ilpv4.connector.it.topologies;

import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.model.settings.ImmutableConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.server.support.Server;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.ServerNode;

public class TopologyUtils {

  static ConnectorSettings defaultConnectorSettings(final InterledgerAddress interledgerAddress) {
    return ImmutableConnectorSettings.builder()
      .ilpAddress(interledgerAddress)
      .build();
  }

  static Server toServerNode(final Graph graph, final InterledgerAddress nodeName) {
    return ((ServerNode) graph.getNode(nodeName.getValue())).getServer();
  }

//  static IlpConnectorNode toConnectorNode(final Graph graph, final InterledgerAddress nodeName) {
//    return ((IlpConnectorNode) graph.getNode(nodeName.getValue()));
//  }

}
