package org.interledger.ilpv4.connector.it.topology.edges;

/**
 * Configures an ILPv4 Connector with a new account.
 *
 * @deprecated Edges are no longer needed since the Topology typically determines who is connected to whom. If new
 * connections need to be created, then tests should grab a node, and utilize the admin API.
 */
@Deprecated
public class ConnectorAccountEdge {//extends Edge {

  //  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  //
  //  private final String nodeKey;
  //  private final AccountSettings accountSettings;
  //  private boolean connected;
  //
  //  public ConnectorAccountEdge(final InterledgerAddress nodeKey, final AccountSettings accountSettings) {
  //    this(nodeKey.getValue(), accountSettings);
  //  }
  //
  //  public ConnectorAccountEdge(final String nodeKey, final AccountSettings accountSettings) {
  //    this.nodeKey = Objects.requireNonNull(nodeKey);
  //    this.accountSettings = Objects.requireNonNull(accountSettings);
  //  }
  //
  //  @Override
  //  public void connect(final Topology topology) {
  //    Objects.requireNonNull(topology);
  //
  //    final BtpServerPluginNode pluginNode = (BtpServerPluginNode) topology.getNode(nodeKey);
  //    final ILPv4Connector ilpConnectorNode = pluginNode.getIlpV4ConnectorNode();
  //    assert ilpConnectorNode != null;
  //
  //    logger.info("Adding account `{}` to ilpConnectorNode...", accountSettings.getInterledgerAddress());
  //    ilpConnectorNode.getAccountManager().addAccount(accountSettings);
  //    logger.info("ilpConnectorNode initialized with account `{}`", accountSettings.getInterledgerAddress());
  //
  //    this.connected = true;
  //  }
  //
  //  public boolean isConnected() {
  //    return this.connected;
  //  }
}