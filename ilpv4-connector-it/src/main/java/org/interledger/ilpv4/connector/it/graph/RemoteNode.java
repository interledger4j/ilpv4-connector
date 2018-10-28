package org.interledger.ilpv4.connector.it.graph;

import java.util.Objects;

public class RemoteNode implements Node {

  private final String scheme;
  private final String host;
  private final int port;

  public RemoteNode(final String scheme, final String host, final int port) {
    this.scheme = Objects.requireNonNull(scheme);
    this.host = Objects.requireNonNull(host);
    this.port = Objects.requireNonNull(port);
  }

  /**
   * The Http scheme this node uses.
   */
  @Override
  public String getScheme() {
    return this.scheme;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }
}
