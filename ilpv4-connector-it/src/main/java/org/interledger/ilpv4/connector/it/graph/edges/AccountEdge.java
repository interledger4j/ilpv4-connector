package org.interledger.ilpv4.connector.it.graph.edges;

import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import org.interledger.ilpv4.connector.it.graph.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Edge;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Connects two ILPv4 nodes together by adding an {@link AccountSettings} to a {@link ConnectorNode}.
 */
public class AccountEdge extends Edge {

  private static final Logger logger = LoggerFactory.getLogger(AccountEdge.class);
  private final String nodeKey;
  private final AccountSettings accountSettings;
  private boolean connected;

  public AccountEdge(final String nodeKey, final AccountSettings accountSettings) {
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