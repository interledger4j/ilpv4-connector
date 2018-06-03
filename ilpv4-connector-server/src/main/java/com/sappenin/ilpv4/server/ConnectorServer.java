package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.server.support.Server;

/**
 * An extension of {@link Server} that implements ILPv4 Connector functionality.
 */
public class ConnectorServer extends Server {

  public ConnectorServer() {
    super(ConnectorServerConfig.class);
  }
}
