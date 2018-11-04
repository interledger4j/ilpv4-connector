package org.interledger.ilpv4.connector.it.graph.edges;

import com.google.common.base.Preconditions;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Edge;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.nodes.IlpSenderNode;
import org.interledger.plugin.lpiv2.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Connects an ILP sender to a remote Connector by adding a {@link org.interledger.plugin.lpiv2.PluginSettings} to the
 * indicated ILP Sender.
 */
public class IlpSenderAccountEdge extends Edge {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String nodeKey;
  private final Plugin pluginDelegate;
  private boolean connected;

  public IlpSenderAccountEdge(final InterledgerAddress nodeKey, final Plugin pluginDelegate) {
    this(nodeKey.getValue(), pluginDelegate);
  }

  public IlpSenderAccountEdge(final String nodeKey, final Plugin pluginDelegate) {
    this.nodeKey = Objects.requireNonNull(nodeKey);
    this.pluginDelegate = Objects.requireNonNull(pluginDelegate);

    pluginDelegate.registerDataHandler((sourceAddress, preparePacket) -> {
      logger.info("Source: {} PreparePacke: {}",
        sourceAddress, preparePacket);
      return null;
    });
    pluginDelegate.registerMoneyHandler((amt) -> {
      logger.info("Amount: {}", amt);
      return null;
    });
  }

  @Override
  public void connect(final Graph graph) {
    Objects.requireNonNull(graph);

    final IlpSenderNode ilpSenderNode = (IlpSenderNode) graph.getNode(nodeKey);
    Preconditions.checkArgument(ilpSenderNode != null, "Couldn't find IlpSender Node using Key: `{}`", nodeKey);

    logger.info("Configuring IlpSender Node (Key=`{}`)...", nodeKey);
    ilpSenderNode.getIlpClient().setPluginDelegate(pluginDelegate);
    try {
      pluginDelegate.connect().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    logger.info("Node initialized: {}...", ilpSenderNode);

    this.connected = true;
  }

  public boolean isConnected() {
    return this.connected;
  }
}