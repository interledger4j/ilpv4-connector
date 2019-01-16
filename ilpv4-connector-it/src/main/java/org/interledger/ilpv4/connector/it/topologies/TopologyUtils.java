package org.interledger.ilpv4.connector.it.topologies;

import com.sappenin.interledger.ilpv4.connector.server.Server;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.interledger.ilpv4.connector.it.topology.ServerNode;

public class TopologyUtils {

//  public static ConnectorSettings defaultConnectorSettings(final InterledgerAddress interledgerAddress) {
//    final SingleAccountConnectionSettings btpSingleAccountConnection =
//      ImmutableSingleAccountConnectionSettings.builder()
//        .bilateralConnectionType(LoopbackConnection.CONNECTION_TYPE)
//        .description("The connection from Alice to this connector.")
//        .accountSettings(ImmutableAccountSettings.builder()
//          .description("The single account between Alice and Connie")
//          .relationship(AccountRelationship.PARENT)
//          .accountAddress(interledgerAddress)
//          .build())
//        .build();
//    final Iterable<? extends SingleAccountConnectionSettings> singleAccountConnections =
//      Lists.newArrayList(btpSingleAccountConnection);
//    return ImmutableConnectorSettings.builder()
//      .nodeIlpAddress(interledgerAddress)
//      .singleAccountConnections(singleAccountConnections)
//      .build();
//  }

  public static Server toServerNode(final Topology topology, final InterledgerAddress nodeName) {
    return ((ServerNode) topology.getNode(nodeName.getValue())).getServer();
  }

  //  static IlpConnectorNode toConnectorNode(final Topology topology, final InterledgerAddress nodeName) {
  //    return ((IlpConnectorNode) topology.getNode(nodeName.getValue()));
  //  }

}
