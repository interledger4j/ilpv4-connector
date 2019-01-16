package org.interledger.ilpv4.connector.it.topology;

import java.util.Objects;

/**
 * An implementation of {@link Node} that can be extended by an ILP client connecting to another node (e.g., a WebSocket
 * client or gRPC client).
 */
public abstract class AbstractClientNode<T> implements ClientNode<T> {

  private final T contentObject;

  public AbstractClientNode(final T contentObject) {
    this.contentObject = Objects.requireNonNull(contentObject);
  }

  /**
   * Accessor for the {@link T} contained in this Node.
   */
  @Override
  public T getContentObject() {
    return contentObject;
  }

  //  /**
  //   * The <tt>scheme</tt> for this server, such as `https`, `ws`, or `wss`, which can be used to make HTTP calls *
  //   * against this server.
  //   */
  //  public String getScheme() {
  //    return this.scheme;
  //  }
  //
  //  /**
  //   * The <tt>host</tt> for this server, such as `localhost` or an IP address, which can be used to make HTTP calls
  //   * against this server.
  //   */
  //  public String getHost() {
  //    return this.host;
  //  }

  //  @Override
  //  public void start() {
  //    server.start();
  //  }
  //
  //  @Override
  //  public void stop() {
  //    server.stop();
  //  }

  //  @Override
  //  public void setPlugin(Plugin<?> plugin) {
  //    throw new RuntimeException("AbstractServerNode does not allow its plugin to be replaced at Runtime!");
  //  }
}
