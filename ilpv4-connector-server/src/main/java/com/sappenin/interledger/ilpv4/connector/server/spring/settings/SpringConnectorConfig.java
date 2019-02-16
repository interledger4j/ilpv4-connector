package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.DefaultILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountSettingsResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.BtpAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountSettingsResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultLinkManager;
import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import com.sappenin.interledger.ilpv4.connector.fx.DefaultExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.fx.ExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.links.ping.PingProtocolLink;
import com.sappenin.interledger.ilpv4.connector.links.ping.PingProtocolLinkFactory;
import com.sappenin.interledger.ilpv4.connector.packetswitch.DefaultILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.packetswitch.InterledgerAddressUtils;
import com.sappenin.interledger.ilpv4.connector.routing.DefaultInternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InMemoryExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.NoOpExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorSettingsFromPropertyFile;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILP;

/**
 * <p>Primary configuration for the ILPv4 Connector.</p>
 *
 * <p>See the package-info in {@link com.sappenin.interledger.ilpv4.connector.server.spring.settings} for more
 * details.</p>
 */
@Configuration
@EnableConfigurationProperties({ConnectorSettingsFromPropertyFile.class})
@ConditionalOnExpression
@Import({
          CodecContextConfig.class,
          // Link-Layer Support
          BlastConfig.class
        })
public class SpringConnectorConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private ApplicationContext applicationContext;

  @PostConstruct
  public void onStartup() {
  }

  @Bean
  EventBus eventBus() {
    return new EventBus();
  }

  /**
   * <p>This is a supplier that can be given to beans for later usage after the application has started. This
   * supplier will not resolve to anything until the `ConnectorSettings` bean has been loaded into the
   * application-context, which occurs via the EnableConfigurationProperties annotation on this class.</p>
   *
   * <p>The normal `ConnectorSettings` will be the one loaded from the Properties files above (see
   * ConnectorSettingsFromPropertyFile). However, for IT purposes, we can optionally use an overrided instance of {@link
   * ConnectorSettings}.</p>
   */
  @Bean
  Supplier<ConnectorSettings> connectorSettingsSupplier() {
    try {
      final Object overrideBean = applicationContext.getBean(ConnectorSettings.OVERRIDE_BEAN_NAME);
      return () -> (ConnectorSettings) overrideBean;
    } catch (Exception e) {
      logger.info("No Override Bean found....");
    }

    // No override was detected, so return the normal variant that exists because of the EnableConfigurationProperties
    // directive above.
    return () -> applicationContext.getBean(ConnectorSettings.class);
  }

  @Bean
  LinkFactoryProvider linkFactoryProvider(
    @Qualifier(ILP) CodecContext ilpCodecContext
  ) {
    final LinkFactoryProvider provider = new LinkFactoryProvider();

    // Register known types...Spring will register proper known types based upon config...
    //provider.registerLinkFactory(LoopBackLink.LINK_TYPE, new LoopbackLinkFactory());
    provider.registerLinkFactory(PingProtocolLink.LINK_TYPE, new PingProtocolLinkFactory(ilpCodecContext));

    // TODO: Register any SPI types..
    // See SPI as well as https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/support/SpringFactoriesLoader.html

    return provider;
  }

  @Bean
  LinkManager linkManager(EventBus eventBus, LinkFactoryProvider linkFactoryProvider) {
    return new DefaultLinkManager(eventBus, linkFactoryProvider);
  }

  @Bean
  AccountManager accountManager(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountIdResolver accountIdResolver, AccountSettingsResolver accountSettingsResolver,
    LinkManager linkManager, EventBus eventBus
  ) {
    return new DefaultAccountManager(connectorSettingsSupplier, accountIdResolver, accountSettingsResolver,
      linkManager, eventBus);
  }

  @Bean
  ExchangeRateService exchangeRateService() {
    return new DefaultExchangeRateService();
  }

  @Bean
  AccountIdResolver accountIdResolver(BtpAccountIdResolver btpAccountIdResolver) {
    return btpAccountIdResolver;
  }

  @Bean
  BtpAccountIdResolver btpAccountIdResolver() {
    return new DefaultAccountIdResolver();
  }

  @Bean
  AccountSettingsResolver accountSettingsResolver(
    Supplier<ConnectorSettings> connectorSettingsSupplier, AccountIdResolver accountIdResolver
  ) {
    return new DefaultAccountSettingsResolver(connectorSettingsSupplier, accountIdResolver);
  }

  @Bean
  public InternalRoutingService internalPaymentRouter() {
    return new DefaultInternalRoutingService();
  }

  @Bean
  @Qualifier("externalPaymentRouter") // This is also a PaymentRouter
  ExternalRoutingService connectorModeRoutingService(
    EventBus eventBus,
    @Qualifier(ILP) CodecContext ilpCodecContext,
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountManager accountManager,
    AccountIdResolver accountIdResolver
  ) {
    return new InMemoryExternalRoutingService(eventBus, ilpCodecContext, connectorSettingsSupplier, accountManager,
      accountIdResolver);
  }

  @Bean
  @Qualifier("externalPaymentRouter") // This is also a PaymentRouter
  @Profile({ConnectorProfile.SINGLE_ACCOUNT_MODE})
  ExternalRoutingService linkModePaymentRoutingService() {
    return new NoOpExternalRoutingService();
  }

  @Bean
  InterledgerAddressUtils interledgerAddressUtils(
    final Supplier<ConnectorSettings> connectorSettingsSupplier, final AccountManager accountManager
  ) {
    return new InterledgerAddressUtils(connectorSettingsSupplier, accountManager);
  }

  @Bean
  ILPv4PacketSwitch ilpPacketSwitch(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    @Qualifier("internalPaymentRouter") PaymentRouter<Route> internalPaymentRouter,
    @Qualifier("externalPaymentRouter") PaymentRouter<Route> externalPaymentRouter,
    ExchangeRateService exchangeRateService,
    AccountManager accountManager,
    InterledgerAddressUtils interledgerAddressUtils
  ) {
    return new DefaultILPv4PacketSwitch(
      connectorSettingsSupplier, internalPaymentRouter, externalPaymentRouter,
      exchangeRateService, accountManager, interledgerAddressUtils
    );
  }

  @Bean
  ILPv4Connector ilpConnector(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountManager accountManager,
    LinkManager linkManager,
    InternalRoutingService internalRoutingService,
    ExternalRoutingService externalRoutingService,
    ILPv4PacketSwitch ilpPacketSwitch,
    EventBus eventBus
  ) {
    return new DefaultILPv4Connector(
      connectorSettingsSupplier,
      accountManager,
      linkManager,
      internalRoutingService, externalRoutingService,
      ilpPacketSwitch,
      eventBus
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
}
