package org.interledger.ilpv4.connector.it.graph;

import com.sappenin.ilpv4.server.support.Server;

import java.util.Objects;

/**
 * An implementation of {@link Node} that contains a {@link Server} for simulating any type of Spring Boot server
 * runtime.
 *
 * @author jfulton
 * @author sappenin
 */
public class ServerNode implements Node {

  private final Server server;

  public ServerNode(final Server server) {
    this.server = Objects.requireNonNull(server);
  }

  @Override
  public String getScheme() {
    return "ws";
  }

  @Override
  public String getHost() {
    return "localhost";
  }

  @Override
  public int getPort() {
    return server.getPort();
  }

  @Override
  public void start() {
    server.start();
  }

  @Override
  public void stop() {
    server.stop();
  }

  public Server getServer() {
    return server;
  }
}
