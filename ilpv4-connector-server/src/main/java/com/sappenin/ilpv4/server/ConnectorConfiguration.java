package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.Connector;
import com.sappenin.ilpv4.DefaultConnector;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.accounts.PropertyBasedAccountManager;
import com.sappenin.ilpv4.connector.routing.InMemoryRoutingTable;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.connector.routing.RoutingTable;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * The root configuration file for the ILPv4 Connector.
 */
@Configuration
@EnableConfigurationProperties(ConnectorSettings.class)
public class ConnectorConfiguration extends WebMvcConfigurerAdapter {

  @Autowired
  ConnectorSettings connectorSettings;

  @Bean
  Connector connector(ConnectorSettings connectorSettings, AccountManager accountManager) {
    return new DefaultConnector(connectorSettings, accountManager);
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
  AccountManager accountManager() {
    return new PropertyBasedAccountManager();
  }
}
