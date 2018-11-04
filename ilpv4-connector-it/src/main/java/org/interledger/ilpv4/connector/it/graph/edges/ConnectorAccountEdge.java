package org.interledger.ilpv4.connector.it.graph.edges;

import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Edge;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.nodes.ConnectorNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Connects two ILPv4 nodes together by adding an {@link AccountSettings} to a {@link ConnectorNode}.
 */
public class ConnectorAccountEdge extends Edge {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String nodeKey;
  private final AccountSettings accountSettings;
  private boolean connected;

  public ConnectorAccountEdge(final InterledgerAddress nodeKey, final AccountSettings accountSettings) {
    this(nodeKey.getValue(), accountSettings);
  }

  public ConnectorAccountEdge(final String nodeKey, final AccountSettings accountSettings) {
    this.nodeKey = Objects.requireNonNull(nodeKey);
    this.accountSettings = Objects.requireNonNull(accountSettings);
  }

  @Override
  public void connect(final Graph graph) {
    Objects.requireNonNull(graph);

    final ConnectorNode connectorNode = (ConnectorNode) graph.getNode(nodeKey);
    assert connectorNode != null;

    logger.info("Adding peer connectorNode {}...", connectorNode);
    connectorNode.getServer().getContext().getBean(AccountManager.class).add(accountSettings);
    logger.info("Node initialized: {}...", connectorNode);

    this.connected = true;
  }

  public boolean isConnected() {
    return this.connected;
  }
}