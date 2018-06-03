package com.sappenin.ilpv4.server.support;

import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

/**
 * Provides settings for a given Connector Server.
 *
 * @author jfulton
 */
public class ConnectorServerSettings {

  private ServletWebServerApplicationContext server;

  /**
   * Required-args Constructor.
   */
  public ConnectorServerSettings(final ServletWebServerApplicationContext server) {
    this.server = server;
  }

  /**
   * Accessor for the port that this server is running on.
   */
  public int getPort() {
    return server.getWebServer().getPort();
  }

}
