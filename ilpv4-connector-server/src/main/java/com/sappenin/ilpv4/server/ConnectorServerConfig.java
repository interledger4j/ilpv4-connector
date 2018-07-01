package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.DefaultIlpConnector;
import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.accounts.DefaultAccountManager;
import com.sappenin.ilpv4.connector.routing.InMemoryRoutingTable;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.connector.routing.RoutingTable;
import com.sappenin.ilpv4.peer.DefaultPeerManager;
import com.sappenin.ilpv4.peer.PeerManager;
import com.sappenin.ilpv4.plugins.DefaultPluginManager;
import com.sappenin.ilpv4.plugins.PluginManager;
import com.sappenin.ilpv4.server.btp.BtpWebSocketConfig;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The root configuration file for the ILPv4 IlpConnector.
 */
@SpringBootApplication
@Import({BtpWebSocketConfig.class})
public class ConnectorServerConfig implements WebMvcConfigurer {

  @Bean
  ConnectorSettings connectorSettings() {
    return new ConnectorSettings();
  }

  @Bean
  IlpConnector connector(ConnectorSettings connectorSettings, PeerManager peerManager) {
    return new DefaultIlpConnector(connectorSettings, peerManager, routingTable, exchangeRateService);
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
  PluginManager pluginManager() {
    return new DefaultPluginManager();
  }

  @Bean
  AccountManager accountManager(ConnectorSettings connectorSettings, PluginManager pluginManager) {
    return new DefaultAccountManager(connectorSettings, pluginManager);
  }

  @Bean
  PeerManager peerManager(AccountManager accountManager, PluginManager pluginManager) {
    return new DefaultPeerManager(accountManager, pluginManager);
  }
}
