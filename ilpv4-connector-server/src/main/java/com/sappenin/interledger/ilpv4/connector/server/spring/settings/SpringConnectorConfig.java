package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.DefaultILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountSettingsResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.BtpAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountSettingsResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultLinkManager;
import com.sappenin.interledger.ilpv4.connector.accounts.InMemoryAccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.balances.InMemoryBalanceTracker;
import com.sappenin.interledger.ilpv4.connector.fx.ExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.fx.JavaMoneyExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.links.DefaultNextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.NextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.filters.LinkFilter;
import com.sappenin.interledger.ilpv4.connector.links.filters.OutgoingBalanceLinkFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.DefaultILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.packetswitch.InterledgerAddressUtils;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.AllowedDestinationPacketFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.BalanceIlpPacketFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.ExpiryPacketFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.MaxPacketAmountFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PeerProtocolPacketFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PingProtocolFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.RateLimitIlpPacketFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.ValidateFulfillmentPacketFilter;
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
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.CCP;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILDCP;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILP;

/**
 * <p>Primary configuration for the ILPv4 Connector.</p>
 *
 * <p>See the package-info in {@link com.sappenin.interledger.ilpv4.connector.server.spring.settings} for more
 * details.</p>
 */
@Configuration
@EnableConfigurationProperties({ConnectorSettingsFromPropertyFile.class})
@Import({
          CodecContextConfig.class,
          // Link-Layer Support
          BlastConfig.class,
        })
public class SpringConnectorConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private ApplicationContext applicationContext;

  @PostConstruct
  public void onStartup() {
    if (connectorSettingsSupplier().get().getOperatorAddress().isPresent()) {
      logger.info("STARTED PEER/PARENT CONNECTOR: `{}`", connectorSettingsSupplier().get().getOperatorAddress().get());
    } else {
      logger.info("STARTED CHILD CONNECTOR: [Operator Address pending IL-DCP]");
    }
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
  LinkFactoryProvider linkFactoryProvider(@Qualifier(ILP) CodecContext ilpCodecContext) {
    final LinkFactoryProvider provider = new LinkFactoryProvider();

    // Register known types...Spring will register proper known types based upon config...
    //provider.registerLinkFactory(LoopBackLink.LINK_TYPE, new LoopbackLinkFactory());
    //provider.registerLinkFactory(PingProtocolLink.LINK_TYPE, new PingProtocolLinkFactory(ilpCodecContext));

    // TODO: Register any SPI types..
    // See SPI as well as https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/support/SpringFactoriesLoader.html

    return provider;
  }

  @Bean
  LinkEventEmitter linkEventEmitter(EventBus eventBus) {
    return new AbstractLink.EventBusEventEmitter(eventBus);
  }

  @Bean
  LinkManager linkManager(EventBus eventBus, LinkFactoryProvider linkFactoryProvider) {
    return new DefaultLinkManager(
      () -> connectorSettingsSupplier().get().getOperatorAddress(),
      eventBus,
      linkFactoryProvider
    );
  }

  @Bean
  AccountManager accountManager(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountIdResolver accountIdResolver, AccountSettingsResolver accountSettingsResolver,
    LinkManager linkManager, EventBus eventBus
  ) {
    return new InMemoryAccountManager(connectorSettingsSupplier, accountIdResolver, accountSettingsResolver,
      linkManager, eventBus);
  }

  @Bean
  ExchangeRateService exchangeRateService() {
    return new JavaMoneyExchangeRateService();
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
  @Qualifier("externalPaymentRouter")
    // This is also a PaymentRouter
  ExternalRoutingService connectorModeRoutingService(
    EventBus eventBus,
    @Qualifier(CCP) CodecContext ccpCodecContext,
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountManager accountManager,
    AccountIdResolver accountIdResolver
  ) {
    return new InMemoryExternalRoutingService(eventBus, ccpCodecContext, connectorSettingsSupplier, accountManager,
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
  BalanceTracker balanceTracker(AccountManager accountManager) {
    return new InMemoryBalanceTracker(accountManager);
  }

  @Bean
  List<PacketSwitchFilter> packetSwitchFilters(
    ExternalRoutingService externalRoutingService, AccountManager accountManager, InterledgerAddressUtils addressUtils,
    BalanceTracker balanceTracker,
    @Qualifier(CCP) CodecContext ccpCodecContext, @Qualifier(ILDCP) CodecContext ildcpCodecContext
  ) {
    final ConnectorSettings connectorSettings = connectorSettingsSupplier().get();
    final Supplier<InterledgerAddress> operatorAddressSupplier =
      () -> connectorSettingsSupplier().get().getOperatorAddressSafe();

    final ImmutableList.Builder<PacketSwitchFilter> filterList =
      ImmutableList.<PacketSwitchFilter>builder().add(
        new RateLimitIlpPacketFilter(operatorAddressSupplier), // Limits Data packets...
        new AllowedDestinationPacketFilter(operatorAddressSupplier, addressUtils),
        new ExpiryPacketFilter(operatorAddressSupplier),
        new MaxPacketAmountFilter(operatorAddressSupplier, accountManager),
        new BalanceIlpPacketFilter(operatorAddressSupplier, balanceTracker),
        new ValidateFulfillmentPacketFilter(operatorAddressSupplier),
        new PeerProtocolPacketFilter(
          operatorAddressSupplier,
          connectorSettingsSupplier().get().getEnabledProtocols(),
          externalRoutingService, accountManager,
          ccpCodecContext, ildcpCodecContext
        )
      );

    // TODO: Throughput for Money...

    final EnabledProtocolSettings enabledProtocolSettings = connectorSettings.getEnabledProtocols();
    /////////////////////////////////////
    // Ping Protocol
    /////////////////////////////////////
    if (enabledProtocolSettings.isPingProtocolEnabled()) {
      filterList.add(new PingProtocolFilter(operatorAddressSupplier));
    }

    /////////////////////////////////////
    // Non-routable destinations (self.*)
    /////////////////////////////////////

    return filterList.build();
  }

  @Bean
  List<LinkFilter> linkFilters(
    AccountManager accountManager, InterledgerAddressUtils addressUtils, BalanceTracker balanceTracker
  ) {
    final Supplier<InterledgerAddress> operatorAddressSupplier =
      () -> connectorSettingsSupplier().get().getOperatorAddress().get();

    return Lists.newArrayList(
      new OutgoingBalanceLinkFilter(operatorAddressSupplier, balanceTracker)
    );
    //      new RateLimitIlpPacketFilter(), // Limits Data packets...
    //      new ExpiryPacketFilter(operatorAddressSupplier),
    //      new MaxPacketAmountFilter(operatorAddressSupplier, accountManager),
    //      new AllowedDestinationPacketFilter(operatorAddressSupplier, addressUtils),
    //      new BalanceIlpPacketFilter(balanceTracker),
    //      // Must be the last filter.
    //      new ValidateFulfillmentPacketFilter(operatorAddressSupplier)
    //      // TODO: Throughput for Money...
    //    );
  }

  @Bean
  NextHopPacketMapper nextHopLinkMapper(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    //@Qualifier("internalPaymentRouter") PaymentRouter<Route> internalPaymentRouter,
    @Qualifier("externalPaymentRouter") PaymentRouter<Route> externalPaymentRouter,
    final ExchangeRateService exchangeRateService,
    final AccountManager accountManager,
    final InterledgerAddressUtils addressUtils
  ) {
    return new DefaultNextHopPacketMapper(
      connectorSettingsSupplier, externalPaymentRouter, exchangeRateService, accountManager,
      addressUtils
    );
  }

  @Bean
  ILPv4PacketSwitch ilpPacketSwitch(
    List<PacketSwitchFilter> packetSwitchFilters, List<LinkFilter> linkFilters,
    AccountManager accountManager, NextHopPacketMapper nextHopPacketMapper
  ) {
    return new DefaultILPv4PacketSwitch(packetSwitchFilters, linkFilters, accountManager, nextHopPacketMapper);
  }

  @Bean
  ILPv4Connector ilpConnector(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountManager accountManager,
    LinkManager linkManager,
    InternalRoutingService internalRoutingService,
    ExternalRoutingService externalRoutingService,
    ILPv4PacketSwitch ilpPacketSwitch,
    BalanceTracker balanceTracker,
    EventBus eventBus
  ) {
    return new DefaultILPv4Connector(
      connectorSettingsSupplier,
      accountManager,
      linkManager,
      internalRoutingService, externalRoutingService,
      ilpPacketSwitch,
      balanceTracker,
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
