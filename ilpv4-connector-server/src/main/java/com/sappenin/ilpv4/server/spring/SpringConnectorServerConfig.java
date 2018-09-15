package com.sappenin.ilpv4.server.spring;

import com.sappenin.ilpv4.DefaultIlpConnector;
import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.accounts.DefaultAccountManager;
import com.sappenin.ilpv4.connector.routing.*;
import com.sappenin.ilpv4.fx.DefaultExchangeRateService;
import com.sappenin.ilpv4.fx.ExchangeRateService;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.settings.ConnectorSettingsFromPropertyFile;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * The root configuration file for the ILPv4 IlpConnector.
 */
@SpringBootApplication
@Import(
  {
    SpringBtpConfig.class,
    SpringAsyncConfig.class
  })
public class SpringConnectorServerConfig implements WebMvcConfigurer {

  @Bean
  ConnectorSettings connectorSettings() {
    return new ConnectorSettingsFromPropertyFile();
  }

  @Bean
  ExchangeRateService exchangeRateService() {
    return new DefaultExchangeRateService();
  }

  @Bean
  IlpConnector connector(
    final ConnectorSettings connectorSettings, final AccountManager accountManager,
    final PaymentRouter<Route> paymentRouter, final ExchangeRateService exchangeRateService) {
    return new DefaultIlpConnector(connectorSettings, accountManager, paymentRouter, exchangeRateService);
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
  AccountManager accountManager(final ConnectorSettings connectorSettings) {
    return new DefaultAccountManager(connectorSettings);
  }

  @Bean
  Executor threadPoolTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(4);
    executor.setThreadNamePrefix("default_task_executor_thread");
    executor.initialize();
    return executor;
  }
}
