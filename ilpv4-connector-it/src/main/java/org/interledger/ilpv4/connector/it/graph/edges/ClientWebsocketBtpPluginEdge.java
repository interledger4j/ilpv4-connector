package org.interledger.ilpv4.connector.it.graph.edges;

import com.google.common.base.Preconditions;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Edge;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.PluginNode;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Represents a connection from a client node (using a {@link ClientWebsocketBtpPlugin}) to a server node that accepts
 * BTPv2 connections.
 *
 * Note that for clients, the plugin is not created until the edge is created, whereas with a server, the plugin is
 * created when the server starts-up.
 */
public class ClientWebsocketBtpPluginEdge extends Edge {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String nodeKey;
  private final ClientWebsocketBtpPlugin plugin;
  private boolean connected;

  /**
   * @param accountAddress The {@link InterledgerAddress} that uniquely identifies the account to connect the contained
   *                       plugin to.
   * @param plugin
   */
  public ClientWebsocketBtpPluginEdge(final InterledgerAddress accountAddress, final ClientWebsocketBtpPlugin plugin) {
    this(accountAddress.getValue(), plugin);
  }

  public ClientWebsocketBtpPluginEdge(final String nodeKey, final ClientWebsocketBtpPlugin plugin) {
    this.nodeKey = Objects.requireNonNull(nodeKey);
    this.plugin = Objects.requireNonNull(plugin);

    plugin.unregisterDataHandler();
    plugin.unregisterMoneyHandler();

    plugin.registerDataHandler((
      //sourceAddress,
      preparePacket) -> {
      logger.info("PreparePacket: {}", preparePacket);
      return null;
    });
    plugin.registerMoneyHandler((amt) -> {
      logger.info("Amount: {}", amt);
      return null;
    });
  }

  @Override
  public void connect(final Graph graph) {
    Objects.requireNonNull(graph);

    logger.info("Configuring ClientWebsocketBtpPluginNode Node (Key=`{}`)...", nodeKey);

    try {
      // Find the node that operates this plugin, and associate the plugin to that node.
      final PluginNode node = (PluginNode) graph.getNode(nodeKey);
      node.setPlugin(plugin);

      // Connect and block...
      plugin.connect().get();
      logger.info("ClientWebsocketBtpPluginNode Node initialized: {}...", node);
      this.connected = true;
      Preconditions.checkArgument(node != null);

    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isConnected() {
    return this.connected;
  }
}