package org.interledger.connector.it.topology;

import org.interledger.connector.server.Server;

import java.util.Objects;

/**
 * An implementation of {@link Node} that contains a {@link Server} for simulating any type of Spring Boot server
 * runtime.
 */
public abstract class AbstractServerNode<S extends Server> extends AbstractNode<S> implements ServerNode<S> {

  private final String scheme;
  private final String host;

  public AbstractServerNode(final S server, final String scheme, String host) {
    super(server);
    this.scheme = Objects.requireNonNull(scheme);
    this.host = Objects.requireNonNull(host);
  }

  /**
   * Accessor for the {@link Server}.
   */
  public S getServer() {
    return getContentObject();
  }

  /**
   * The <tt>scheme</tt> for this server, such as `https`, `ws`, or `wss`, which can be used to make HTTP calls *
   * against this server.
   */
  public String getScheme() {
    return this.scheme;
  }

  /**
   * The <tt>host</tt> for this server, such as `localhost` or an IP address, which can be used to make HTTP calls
   * against this server.
   */
  public String getHost() {
    return this.host;
  }

  @Override
  public void start() {
    getServer().start();
  }

  @Override
  public void stop() {
    getServer().stop();
  }
}
