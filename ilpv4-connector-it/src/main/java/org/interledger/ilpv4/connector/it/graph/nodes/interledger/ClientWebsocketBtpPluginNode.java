package org.interledger.ilpv4.connector.it.graph.nodes.interledger;

import com.google.common.base.Preconditions;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.BtpClientPluginNode;
import org.interledger.ilpv4.connector.it.graph.PluginNode;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;

import java.util.Objects;

/**
 * An implementation of {@link PluginNode} that contains a {@link Plugin} of a particular type. This node is useful for
 * constructing nodes that don't need a full Spring Boot server runtime, but still can speak a bilateral Interledger
 * protocol via the {@link Plugin} interface.
 */
public class ClientWebsocketBtpPluginNode implements BtpClientPluginNode<ClientWebsocketBtpPlugin> {

  private Plugin<?> clientPlugin;

  /**
   * No-args constructor.
   */
  public ClientWebsocketBtpPluginNode() {
  }

  @Override
  public void start() {
    // When a client node is started, it's a no-op because the edge will initiate the connection.
  }

  @Override
  public void stop() {
    getContentObject().disconnect().join();
  }

  /**
   * Accessor for the {@link ClientWebsocketBtpPlugin} contained in this Node.
   */
  @Override
  public ClientWebsocketBtpPlugin getContentObject() {
    if (clientPlugin == null) {
      throw new RuntimeException("You must add an edge to this node before accessing its content object!");
    } else {
      return (ClientWebsocketBtpPlugin) clientPlugin;
    }
  }

  @Override
  public String toString() {
    return clientPlugin.getPluginSettings().getPeerAccountAddress().toString();
  }

  @Override
  public Plugin<?> getPlugin(InterledgerAddress accountAddress) {
    return this.clientPlugin;
  }

  @Override
  public void setPlugin(final Plugin<?> clientPlugin) {
    Preconditions.checkArgument(ClientWebsocketBtpPlugin.class.isAssignableFrom(clientPlugin.getClass()));
    this.clientPlugin = Objects.requireNonNull(clientPlugin);
  }
}