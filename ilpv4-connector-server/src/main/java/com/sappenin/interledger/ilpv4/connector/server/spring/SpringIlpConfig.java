package com.sappenin.interledger.ilpv4.connector.server.spring;

import com.sappenin.interledger.ilpv4.connector.DefaultILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultPluginManager;
import com.sappenin.interledger.ilpv4.connector.fx.DefaultExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.fx.ExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.packetswitch.DefaultILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.plugins.IlpPluginFactory;
import com.sappenin.interledger.ilpv4.connector.routing.DefaultRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InMemoryRoutingTable;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingTable;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettingsFromPropertyFile;
import org.interledger.encoding.asn.framework.CodecContext;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.server.spring.CodecContextConfig.BTP;
import static com.sappenin.interledger.ilpv4.connector.server.spring.CodecContextConfig.ILP;

/**
 * Primary configuration for the ILPv4 Connector.
 */
@Configuration
@Import(
  {
    CodecContextConfig.class,
    SpringBtpConfig.class,
    SpringWsConfig.class,
    SpringConnectorWebMvc.class,
    //SpringAsyncConfig.class
  })
public class SpringIlpConfig {

  @Autowired
  private ApplicationContext applicationContext;

  @PostConstruct
  public void startup() {
  }

  // Initial Connector Settings. May be replaced at Runtime, so all reliance should be done on the Supplier below, as
  // this value may not be correct after server initialization.
  @Bean
    //(name = ConnectorSettings.BEAN_NAME)
  ConnectorSettings connectorSettings() {
    return new ConnectorSettingsFromPropertyFile();
  }

  @Bean
  Supplier<ConnectorSettings> connectorSettingsSupplier() {
    // This is necessary to allow for Runtime-reloading of ConnectorSettings via the Application Context.
    return () -> applicationContext.getBean(ConnectorSettings.class);
  }

  @Bean
  BtpAuthenticationService btpAuthenticationService() {
    // TODO: Implement this!
    return new BtpAuthenticationService.NoOpBtpAuthenticationService();
  }

  @Bean
  IlpPluginFactory ilpPluginFactory(
    @Qualifier(ILP) final CodecContext ilpCodecContext,
    @Qualifier(BTP) final CodecContext btpCodecContext,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpAuthenticationService btpAuthenticationService,
    final ApplicationEventPublisher eventPublisher
  ) {
    return new IlpPluginFactory(
      ilpCodecContext, btpCodecContext, binaryMessageToBtpPacketConverter, btpPacketToBinaryMessageConverter,
      btpAuthenticationService, eventPublisher
    );
  }

  @Bean
  AccountManager.PluginManager pluginManager(final IlpPluginFactory ilpPluginFactory) {
    return new DefaultPluginManager(ilpPluginFactory);
  }

  @Bean
  AccountManager accountManager(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager.PluginManager pluginManager
  ) {
    return new DefaultAccountManager(connectorSettingsSupplier, pluginManager);
  }

  @Bean
  RoutingTable<Route> routeRoutingTable() {
    return new InMemoryRoutingTable();
  }

  @Bean
  ExchangeRateService exchangeRateService() {
    return new DefaultExchangeRateService();
  }

  @Bean
  RoutingService routingService(
    @Qualifier(ILP) final CodecContext ilpCodecContext,
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager
  ) {
    return new DefaultRoutingService(ilpCodecContext, connectorSettingsSupplier, accountManager);
  }

  @Bean
  ILPv4PacketSwitch ilpPacketSwitch(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final PaymentRouter<Route> paymentRouter,
    final ExchangeRateService exchangeRateService,
    final AccountManager accountManager
  ) {
    return new DefaultILPv4PacketSwitch(connectorSettingsSupplier, paymentRouter, exchangeRateService,
      accountManager);
  }

  @Bean
  ILPv4Connector ilpConnector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final RoutingService routingService,
    final ILPv4PacketSwitch ilpPacketSwitch
  ) {
    // All initialization is performed in DefaultILPv4Connector#init
    return new DefaultILPv4Connector(connectorSettingsSupplier, accountManager, routingService, ilpPacketSwitch);
  }

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
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