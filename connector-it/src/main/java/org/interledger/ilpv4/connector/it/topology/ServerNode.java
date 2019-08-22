package org.interledger.ilpv4.connector.it.topology;

import com.sappenin.interledger.ilpv4.connector.server.Server;
import okhttp3.HttpUrl;

/**
 * An extension of {@link Node} whose contained object is an instance of {@link Server}, which is useful for simulating
 * any type of Spring Boot server runtime, such as for a Connector.
 *
 * @param <S> The type of Server that this server-node exposes (e.g., `ConnectorServer` for a connector).
 */
public interface ServerNode<S extends Server> extends Node<S> {

  /**
   * The <tt>scheme</tt> for this server, such as `https`, `ws`, or `wss`, which can be used to make HTTP calls *
   * against this server.
   */
  String getScheme();

  /**
   * The <tt>host</tt> for this server, such as `localhost` or an IP address, which can be used to make HTTP calls
   * against this server.
   */
  default String getHost() {
    return "localhost";
  }

  /**
   * The <tt>port</tt> for this server, such as `localhost` or an IP address, which can be used to make HTTP calls
   * against this server.
   */
  default int getPort() {
    return getServer().getPort();
  }

  @Override
  default void start() {
    getServer().start();
  }

  @Override
  default void stop() {
    getServer().stop();
  }

  /**
   * Get this node's endpoint location as an HTTP URL.
   */
  default HttpUrl getNodeUrl() {
    return HttpUrl.parse(getHost()).newBuilder().port(getPort()).build();
  }

  /**
   * Accessor for the Spring server powering this ILP Node.
   */
  S getServer();
}
