package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.DefaultIlpConnector;
import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.accounts.DefaultAccountManager;
import com.sappenin.ilpv4.connector.routing.*;
import com.sappenin.ilpv4.fx.DefaultExchangeRateService;
import com.sappenin.ilpv4.fx.ExchangeRateService;
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
  ExchangeRateService exchangeRateService() {
    return new DefaultExchangeRateService();
  }

  @Bean
  IlpConnector connector(
    final ConnectorSettings connectorSettings, final PeerManager peerManager,
    final PaymentRouter<Route> paymentRouter, final ExchangeRateService exchangeRateService) {
    return new DefaultIlpConnector(connectorSettings, peerManager, paymentRouter, exchangeRateService);
  }

  @Bean
  RoutingTable<Route> routeRoutingTable() {
    return new InMemoryRoutingTable();
  }

  @Bean
  PaymentRouter<Route> paymentRouter(final RoutingTable<Route> routingTable) {
    return new SimplePaymentRouter(routingTable);
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
  PeerManager peerManager(AccountManager accountManager) {
    return new DefaultPeerManager(accountManager);
  }
}
