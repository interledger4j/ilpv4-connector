package org.interledger.ilpv4.connector.it.graph.edges;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Edge;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.nodes.interledger.BtpServerPluginNode;
import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Configures an ILPv4 Connector with a new account.
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

    final BtpServerPluginNode pluginNode = (BtpServerPluginNode) graph.getNode(nodeKey);
    final ILPv4Connector ilpConnectorNode = pluginNode.getIlpV4ConnectorNode();
    assert ilpConnectorNode != null;

    logger.info("Adding account `{}` to ilpConnectorNode...", accountSettings.getInterledgerAddress());
    ilpConnectorNode.getAccountManager().add(accountSettings);
    logger.info("ilpConnectorNode initialized with account `{}`", accountSettings.getInterledgerAddress());

    this.connected = true;
  }

  public boolean isConnected() {
    return this.connected;
  }
}