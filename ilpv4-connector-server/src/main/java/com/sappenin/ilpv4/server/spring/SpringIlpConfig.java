package com.sappenin.ilpv4.server.spring;

import com.sappenin.ilpv4.DefaultIlpConnector;
import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.accounts.DefaultAccountManager;
import com.sappenin.ilpv4.connector.routing.*;
import com.sappenin.ilpv4.fx.DefaultExchangeRateService;
import com.sappenin.ilpv4.fx.ExchangeRateService;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.packetswitch.DefaultIlpPacketSwitch;
import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import com.sappenin.ilpv4.packetswitch.preemptors.EchoController;
import com.sappenin.ilpv4.plugins.IlpPluginFactory;
import com.sappenin.ilpv4.settings.ConnectorSettingsFromPropertyFile;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.sappenin.ilpv4.server.spring.CodecContextConfig.BTP;
import static com.sappenin.ilpv4.server.spring.CodecContextConfig.ILP;

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
  Environment environment;

  @Autowired
  ApplicationContext applicationContext;

  // Initial Connector Settings. May be replaced at Runtime, so all reliance should be done on the Supplier below, as
  // this value may not be correct after server initialization.
  @Bean
  ConnectorSettings connectorSettings() {
    return new ConnectorSettingsFromPropertyFile();
  }

  @Bean
  Supplier<ConnectorSettings> connectorSettingsSupplier() {
    // This is necessary to allow for Runtime-reloading of ConnectorSettings via the Application Context.
    return () -> applicationContext.getBean(ConnectorSettings.class);
  }

  //  @Bean
  //  IlpLogUtils ilpLogUtils(
  //    @Qualifier(BTP) final CodecContext btpCodecContext,
  //    @Qualifier(ILP) final CodecContext ilpCodecContext
  //  ) {
  //    return new IlpLogUtils(btpCodecContext, ilpCodecContext);
  //  }

  @Bean
  RoutingTable<Route> routeRoutingTable() {
    return new InMemoryRoutingTable();
  }

  @Bean
  PaymentRouter<Route> paymentRouter(final RoutingTable<Route> routingTable) {
    return new SimplePaymentRouter(routingTable);
  }

  @Bean
  ExchangeRateService exchangeRateService() {
    return new DefaultExchangeRateService();
  }

  @Bean
  EchoController echoController() {
    return new EchoController(
      CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
    );
  }

  @Bean
  IlpPacketSwitch ilpPacketSwitch(
    final ConnectorSettings connectorSettings,
    final PaymentRouter<Route> paymentRouter,
    final ExchangeRateService exchangeRateService,
    final AccountManager accountManager,
    final EchoController echoController
  ) {
    return new DefaultIlpPacketSwitch(connectorSettings, paymentRouter, exchangeRateService, accountManager,
      echoController);
  }

  @Bean
  IlpConnector ilpConnector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final PaymentRouter<Route> paymentRouter,
    final IlpPacketSwitch ilpPacketSwitch
    //    final Plugin.IlpDataHandler ilpDataHandler,
    //    final Plugin.IlpMoneyHandler ilpMoneyHandler
  ) {
    return new DefaultIlpConnector(
      connectorSettingsSupplier, accountManager, paymentRouter, ilpPacketSwitch
      //ilpDataHandler,
      //ilpMoneyHandler
    );
  }

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  @Lazy
  IlpPluginFactory ilpPluginFactory(
    @Qualifier(ILP) final CodecContext ilpCodecContext,
    @Qualifier(BTP) final CodecContext btpCodecContext,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    @Lazy final IlpPacketSwitch ilpPacketSwitch
  ) {
    return new IlpPluginFactory(
      ilpCodecContext, btpCodecContext, btpSubProtocolHandlerRegistry, binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter, ilpPacketSwitch
    );
  }

  @Bean
  AccountManager accountManager(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final IlpPluginFactory ilpPluginFactory,
    final PaymentRouter<Route> paymentRouter
    //@Lazy final Plugin.IlpDataHandler ilpDataHandler,
    //@Lazy final Plugin.IlpMoneyHandler ilpMoneyHandler
  ) {
    return new DefaultAccountManager(
      connectorSettingsSupplier, ilpPluginFactory, paymentRouter
      //ilpDataHandler, ilpMoneyHandler
    );
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

  @PostConstruct
  public void startup() {
  }

}