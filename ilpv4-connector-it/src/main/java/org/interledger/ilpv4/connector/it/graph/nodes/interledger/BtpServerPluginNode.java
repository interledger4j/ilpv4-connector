package org.interledger.ilpv4.connector.it.graph.nodes.interledger;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.AbstractServerNode;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import org.interledger.plugin.lpiv2.btp2.spring.ServerWebsocketBtpPlugin;

import java.util.Objects;

/**
 * A graph-node that wraps a Spring Boot Server runtime an ILPv4 Connector that has a single BTP server plugin that
 * accepts BTP connections.
 */
public class BtpServerPluginNode extends AbstractServerNode<ConnectorServer> {

  private static final String CONNECTOR_BEAN = "ilpConnector";

  // This node exposes, by default, a single server plugin, based upon this address.
  private final InterledgerAddress pluginAddress;

  /**
   * Required-args Constructor.
   *
   * @param pluginAddress The {@link InterledgerAddress} of the plugin that this node wraps.
   */
  public BtpServerPluginNode(final InterledgerAddress pluginAddress, final ConnectorServer server) {
    super(server, "ws", "localhost");
    this.pluginAddress = Objects.requireNonNull(pluginAddress);
  }

  @Override
  public String toString() {
    return getILPv4Connector().getAddress().toString();
  }

  /**
   * The Spring server contains the content-object, so we look in there to find and return it.
   */
  public ILPv4Connector getIlpV4ConnectorNode() {
    return (ILPv4Connector) this.getServer().getContext().getBean(CONNECTOR_BEAN);
  }

  /**
   * Accessor for the plugin identified by {@code accountAddress}.
   *
   * @param accountAddress
   */
  @Override
  public ServerWebsocketBtpPlugin getPlugin(InterledgerAddress accountAddress) {
    final AccountManager accountManager = ((AccountManager) this.getServer().getContext().getBean("accountManager"));
    return (ServerWebsocketBtpPlugin) accountManager.getPluginManager().safeGetPlugin(accountAddress);
  }

  /**
   * This node contains a Spring Server, so to return its {@link ILPv4Connector} we need to inspect the
   * application-context.
   */
  public ILPv4Connector getILPv4Connector() {
    return this.getServer().getContext().getBean(ILPv4Connector.class);
  }

}
