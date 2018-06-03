package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.DefaultIlpConnector;
import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.connector.routing.InMemoryRoutingTable;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.connector.routing.RoutingTable;
import com.sappenin.ilpv4.peer.PeerManager;
import com.sappenin.ilpv4.peer.PropertyBasedPeerManager;
import com.sappenin.ilpv4.server.btp.BtpWebSocketConfig;
import com.sappenin.ilpv4.server.support.ConnectorServerSettings;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The root configuration file for the ILPv4 IlpConnector.
 */
@SpringBootApplication
@Import({BtpWebSocketConfig.class})
@EnableConfigurationProperties({ConnectorSettings.class})
public class ConnectorServerConfig implements WebMvcConfigurer {

  @Autowired
  private ConnectorSettings connectorSettings;

  @Autowired
  private ServletWebServerApplicationContext server;

  @Bean
  public ConnectorServerSettings settings() {
    return new ConnectorServerSettings(server);
  }

  @Bean
  IlpConnector connector(ConnectorSettings connectorSettings, PeerManager peerManager) {
    return new DefaultIlpConnector(connectorSettings, peerManager);
  }

  @Bean
  RoutingTable<Route> routeRoutingTable() {
    return new InMemoryRoutingTable();
  }

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  PeerManager accountManager() {
    return new PropertyBasedPeerManager();
  }
}
