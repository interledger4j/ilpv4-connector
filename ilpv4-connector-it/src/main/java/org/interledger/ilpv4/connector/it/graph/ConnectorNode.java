package org.interledger.ilpv4.connector.it.graph;

import com.sappenin.ilpv4.peer.PeerManager;
import com.sappenin.ilpv4.server.support.Server;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.interledger.ilpv4.connector.it.graph.edges.PeeringEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * An implementation of {@link Node} that contains a {@link Server} running an ILP Connectors.
 */
public class ConnectorNode extends ServerNode implements Node {

  private static final Logger logger = LoggerFactory.getLogger(PeeringEdge.class);

  private final ConnectorSettings connectorSettings;

  /**
   * Required-args Constructor.
   *
   * @param server
   * @param connectorSettings
   */
  public ConnectorNode(final Server server, final ConnectorSettings connectorSettings) {
    super(server);
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
  }

  public ConnectorSettings getConnectorSettings() {
    return connectorSettings;
  }

  @Override
  public String toString() {
    return this.getConnectorSettings().getIlpAddress();
  }

  /**
   * Configure this Connector. This method should be called once all of the other ConnectorNodes in the Graph have been
   * started (e.g., peering should not occur until all servers have started).
   */
  public void initialize() {
    logger.info("Initializing peers for connectorNode {}...", this);
    final PeerManager peerManager = this.getServer().getContext().getBean(PeerManager.class);
    this.getConnectorSettings().getPeers().stream()
      .map(ConnectorSettings.PeerSettings::toPeer)
      .forEach(peerManager::add);
    logger.info("Node initialized: {}...", this);
  }
}
