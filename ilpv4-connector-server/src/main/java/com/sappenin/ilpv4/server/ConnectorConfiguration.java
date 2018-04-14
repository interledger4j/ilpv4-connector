package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.connector.routing.InMemoryRoutingTable;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.connector.routing.RoutingTable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * The root configuration file for the ILPv4 Connector.
 */
@Configuration
//@EnableConfigurationProperties(SpringLedgerPluginConfigProperties.class)
public class ConnectorConfiguration extends WebMvcConfigurerAdapter {

  @Bean
  RoutingTable<Route> routeRoutingTable() {
    return new InMemoryRoutingTable();
  }

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
