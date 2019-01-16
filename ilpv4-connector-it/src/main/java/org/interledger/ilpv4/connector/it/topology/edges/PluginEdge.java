package org.interledger.ilpv4.connector.it.topology.edges;

import com.google.common.base.Preconditions;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.topology.Edge;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.interledger.ilpv4.connector.it.topology.PluginNode;
import org.interledger.plugin.lpiv2.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Represents a connection from a node using a {@link Plugin} to a server node that accepts BTPv2 connections.
 *
 * Note that for clients, the plugin is not created until the edge is created, whereas with a server, the plugin is
 * created when the server starts-up.
 *
 * @deprecated Edges are no longer needed since the Topology typically determines who is connected to whom. If new
 * connections need to be created, then tests should grab a node, and utilize the admin API.
 */
@Deprecated
public class PluginEdge extends Edge {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String nodeKey;
  private final Plugin plugin;
  private boolean connected;

  /**
   * @param accountAddress The {@link InterledgerAddress} that uniquely identifies the account to connect the contained
   *                       plugin to.
   * @param plugin
   */
  public PluginEdge(final InterledgerAddress accountAddress, final Plugin plugin) {
    this(accountAddress.getValue(), plugin);
  }

  public PluginEdge(final String nodeKey, final Plugin plugin) {
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
  public void connect(final Topology topology) {
    Objects.requireNonNull(topology);

    logger.info("Configuring ClientWebsocketBtpPluginNode Node (Key=`{}`)...", nodeKey);

    try {
      // Find the node that operates this plugin, and associate the plugin to that node.
      final PluginNode node = (PluginNode) topology.getNode(nodeKey);
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