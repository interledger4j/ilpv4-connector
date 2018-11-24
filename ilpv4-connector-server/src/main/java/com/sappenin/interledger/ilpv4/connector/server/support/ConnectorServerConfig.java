package com.sappenin.interledger.ilpv4.connector.server.support;

import com.sappenin.interledger.ilpv4.connector.server.spring.SpringIlpConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Initial Config for an ILP Connector Server.
 */
@SpringBootApplication
@Import({SpringIlpConfig.class})
public class ConnectorServerConfig {

  private ServletWebServerApplicationContext server;

  /**
   * Required-args Constructor.
   */
  public ConnectorServerConfig(final ServletWebServerApplicationContext server) {
    this.server = server;
  }

  /**
   * Accessor for the port that this server is running on.
   */
  public int getPort() {
    return server.getWebServer().getPort();
  }

}
