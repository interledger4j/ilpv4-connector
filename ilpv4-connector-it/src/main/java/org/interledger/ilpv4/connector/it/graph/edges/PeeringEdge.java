package org.interledger.ilpv4.connector.it.graph.edges;

import com.sappenin.ilpv4.model.Peer;
import com.sappenin.ilpv4.peer.PeerManager;
import org.interledger.ilpv4.connector.it.graph.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Edge;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Connects two ILPv4 nodes together by adding a {@link Peer} to a {@link ConnectorNode}.
 */
public class PeeringEdge extends Edge {

  private static final Logger logger = LoggerFactory.getLogger(PeeringEdge.class);

  private final String nodeKey;
  private final List<Peer> peers;

  public PeeringEdge(final String nodeKey, final List<Peer> peers) {
    this.nodeKey = Objects.requireNonNull(nodeKey);
    this.peers = Objects.requireNonNull(peers);
  }

  @Override
  public void connect(final Graph graph) {
    Objects.requireNonNull(graph);

    final ConnectorNode connectorNode = (ConnectorNode) graph.getNode(nodeKey);
    assert connectorNode != null;

    logger.info("Adding peer connectorNode {}...", connectorNode);
    peers.stream().forEach(peer ->
      connectorNode.getServer().getContext().getBean(PeerManager.class).add(peer)
    );
    logger.info("Node initialized: {}...", connectorNode);
  }

}