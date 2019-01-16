package org.interledger.ilpv4.connector.it.topology;

import com.sappenin.interledger.ilpv4.connector.server.Server;

import java.util.Objects;

/**
 * An implementation of {@link Node} that contains a {@link Server} for simulating any type of Spring Boot server
 * runtime.
 */
public abstract class AbstractServerNode<S extends Server> implements ServerNode<S> {

  private final S server;
  private final String scheme;
  private final String host;

  public AbstractServerNode(final S server) {
    this(server, "https", "localost");
  }

  public AbstractServerNode(final S server, final String scheme, String host) {
    this.server = Objects.requireNonNull(server);
    this.scheme = Objects.requireNonNull(scheme);
    this.host = Objects.requireNonNull(host);
  }

  /**
   * Accessor for the {@link Server}.
   */
  public S getServer() {
    return server;
  }

  /**
   * Accessor for the {@link S} contained in this Node.
   */
  @Override
  public S getContentObject() {
    return getServer();
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
    server.start();
  }

  @Override
  public void stop() {
    server.stop();
  }

//  @Override
//  public void setPlugin(Plugin<?> plugin) {
//    throw new RuntimeException("AbstractServerNode does not allow its plugin to be replaced at Runtime!");
//  }
}
