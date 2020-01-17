package org.interledger.connector.server;

import org.interledger.connector.server.spring.settings.SpringConnectorConfig;
import org.interledger.connector.server.spring.settings.web.FixContentTypeFilter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.servlet.Filter;

/**
 * Initial Config for an ILP Connector Server.
 */
// A convenience annotation that adds all of the following:
// @Configuration, @EnableAutoConfiguration, @EnableWebMvc,and @ComponentScan
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class) // Excluded for `problems` support
@Import({SpringConnectorConfig.class})
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

  @Bean
  public Filter filter() {
    return new FixContentTypeFilter();
  }

}
