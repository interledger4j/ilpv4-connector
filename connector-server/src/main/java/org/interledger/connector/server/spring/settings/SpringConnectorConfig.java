package org.interledger.connector.server.spring.settings;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.interledger.connector.ConnectorExceptionHandler;
import org.interledger.connector.DefaultILPv4Connector;
import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.accounts.AccountIdResolver;
import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.accounts.AccountSettingsResolver;
import org.interledger.connector.accounts.BtpAccountIdResolver;
import org.interledger.connector.accounts.DefaultAccountIdResolver;
import org.interledger.connector.accounts.DefaultAccountManager;
import org.interledger.connector.accounts.DefaultAccountSettingsResolver;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.fx.JavaMoneyUtils;
import org.interledger.connector.links.DefaultLinkManager;
import org.interledger.connector.links.DefaultLinkSettingsFactory;
import org.interledger.connector.links.DefaultNextHopPacketMapper;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.connector.links.filters.OutgoingBalanceLinkFilter;
import org.interledger.connector.links.loopback.LoopbackLink;
import org.interledger.connector.links.loopback.LoopbackLinkFactory;
import org.interledger.connector.links.ping.PingLoopbackLink;
import org.interledger.connector.links.ping.PingLoopbackLinkFactory;
import org.interledger.connector.packetswitch.DefaultILPv4PacketSwitch;
import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.connector.packetswitch.InterledgerAddressUtils;
import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.connector.packetswitch.filters.AllowedDestinationPacketFilter;
import org.interledger.connector.packetswitch.filters.BalanceIlpPacketFilter;
import org.interledger.connector.packetswitch.filters.ExpiryPacketFilter;
import org.interledger.connector.packetswitch.filters.MaxPacketAmountFilter;
import org.interledger.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.connector.packetswitch.filters.PeerProtocolPacketFilter;
import org.interledger.connector.packetswitch.filters.RateLimitIlpPacketFilter;
import org.interledger.connector.packetswitch.filters.ValidateFulfillmentPacketFilter;
import org.interledger.connector.routing.ChildAccountPaymentRouter;
import org.interledger.connector.routing.DefaultRouteBroadcaster;
import org.interledger.connector.routing.ExternalRoutingService;
import org.interledger.connector.routing.ForwardingRoutingTable;
import org.interledger.connector.routing.InMemoryExternalRoutingService;
import org.interledger.connector.routing.InMemoryForwardingRoutingTable;
import org.interledger.connector.routing.RouteBroadcaster;
import org.interledger.connector.routing.RouteUpdate;
import org.interledger.connector.server.spring.settings.crypto.CryptoConfig;
import org.interledger.connector.server.spring.settings.javamoney.JavaMoneyConfig;
import org.interledger.connector.server.spring.settings.properties.ConnectorSettingsFromPropertyFile;
import org.interledger.connector.server.spring.settings.web.SpringConnectorWebMvc;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.settlement.SettlementService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.Decryptor;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.connector.config.BalanceTrackerConfig;
import org.interledger.connector.config.CaffeineCacheConfig;
import org.interledger.connector.config.RedisConfig;
import org.interledger.connector.config.SettlementConfig;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;

/**
 * <p>Primary configuration for the Connector.</p>
 *
 * <p>See the package-info in {@link org.interledger.connector.server.spring.settings} for more
 * details.</p>
 */
@Configuration
@EnableConfigurationProperties({ConnectorSettingsFromPropertyFile.class})
@Import({
          JavaMoneyConfig.class,
          CodecContextConfig.class,
          ConnectorPersistenceConfig.class,
          CryptoConfig.class,
          ResiliencyConfig.class,
          CaffeineCacheConfig.class,
          RedisConfig.class, SettlementConfig.class, BalanceTrackerConfig.class,
          SpringConnectorWebMvc.class
        })
public class SpringConnectorConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private ApplicationContext applicationContext;

  /**
   * All internal Connector events propagate locally in this JVM using this EventBus.
   */
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
      logger.debug("No ConnectorSettings Override Bean found....");
    }

    // No override was detected, so return the normal variant that exists because of the EnableConfigurationProperties
    // directive above.
    return () -> applicationContext.getBean(ConnectorSettings.class);
  }

  @Bean
  LoopbackLinkFactory loopbackLinkFactory(LinkEventEmitter linkEventEmitter, PacketRejector packetRejector) {
    return new LoopbackLinkFactory(linkEventEmitter, packetRejector);
  }

  @Bean
  PingLoopbackLinkFactory unidirectionalPingLinkFactory(LinkEventEmitter linkEventEmitter) {
    return new PingLoopbackLinkFactory(linkEventEmitter);
  }

  @Bean
  LinkFactoryProvider linkFactoryProvider(
    LoopbackLinkFactory loopbackLinkFactory, PingLoopbackLinkFactory pingLoopbackLinkFactory
  ) {
    final LinkFactoryProvider provider = new LinkFactoryProvider();

    // Register known types...Spring will register proper known types based upon config...
    provider.registerLinkFactory(LoopbackLink.LINK_TYPE, loopbackLinkFactory);
    provider.registerLinkFactory(PingLoopbackLink.LINK_TYPE, pingLoopbackLinkFactory);

    // TODO: Register any SPI types...?
    // See https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/support/SpringFactoriesLoader.html

    return provider;
  }

  @Bean
  LinkSettingsFactory linkSettingsFactory() {
    return new DefaultLinkSettingsFactory();
  }

  @Bean
  LinkEventEmitter linkEventEmitter(EventBus eventBus) {
    return new AbstractLink.EventBusEventEmitter(eventBus);
  }

  @Bean
  LinkManager linkManager(
    EventBus eventBus,
    AccountSettingsRepository accountSettingsRepository,
    LinkSettingsFactory linkSettingsFactory,
    LinkFactoryProvider linkFactoryProvider,
    AccountIdResolver accountIdResolver,
    CircuitBreakerConfig circuitBreakerConfig
  ) {
    return new DefaultLinkManager(
      () -> connectorSettingsSupplier().get().getOperatorAddress(),
      accountSettingsRepository,
      linkSettingsFactory,
      linkFactoryProvider,
      accountIdResolver,
      circuitBreakerConfig,
      eventBus
    );
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
  AccountManager accountManager(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountSettingsRepository accountSettingsRepository,
    LinkManager linkManager,
    ConversionService conversionService,
    SettlementEngineClient settlementEngineClient
  ) {
    return new DefaultAccountManager(
      connectorSettingsSupplier, conversionService, accountSettingsRepository, linkManager, settlementEngineClient
    );
  }

  @Bean
  AccountSettingsResolver accountSettingsResolver(
    AccountSettingsRepository accountSettingsRepository, AccountIdResolver accountIdResolver
  ) {
    return new DefaultAccountSettingsResolver(accountSettingsRepository, accountIdResolver);
  }

  @Bean
  InMemoryForwardingRoutingTable InMemoryRouteUpdateForwardRoutingTable() {
    return new InMemoryForwardingRoutingTable();
  }

  @Bean
  ChildAccountPaymentRouter childAccountPaymentRouter(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountSettingsRepository accountSettingsRepository,
    final Decryptor decryptor
  ) {

    // If the Ping Protocol is enabled, we need to ensure that there is a Ping account suitable to accept value for
    // Ping requests.
    if (connectorSettingsSupplier.get().getEnabledProtocols().isPingProtocolEnabled() &&
      !accountSettingsRepository.findByAccountId(PING_ACCOUNT_ID).isPresent()) {
      // Create this account.

      final AccountSettings pingAccountSettings = AccountSettings.builder()
        .accountId(PING_ACCOUNT_ID)
        .accountRelationship(AccountRelationship.CHILD)
        .assetCode("USD") // TODO: Make this configurable, or else the same as the Connector's base currency.
        .assetScale(2) // TODO: Make this configurable, or else the same as the Connector's base currency.
        .description("A receiver-like child account for collecting all Ping protocol revenues.")
        // TODO: In theory we don't need a rate limit for ping requests because they should always contain value.
        //  However, some systems may ping with a 0-value packet. Also, consider the case where 1M accounts each
        //  ping a Connector cluster every 5 or 10 or 60 seconds.
        .rateLimitSettings(AccountRateLimitSettings.builder()
          .maxPacketsPerSecond(1) // TODO: Make Configurable, per the above comment.
          .build())
        .linkType(PingLoopbackLink.LINK_TYPE)
        .build();

      accountSettingsRepository.save(new AccountSettingsEntity(pingAccountSettings));
    }

    return new ChildAccountPaymentRouter(connectorSettingsSupplier, accountSettingsRepository, decryptor);
  }

  @Bean
  ExternalRoutingService externalRoutingService(
    final EventBus eventBus,
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final Decryptor decryptor,
    final AccountSettingsRepository accountSettingsRepository,
    final ChildAccountPaymentRouter childAccountPaymentRouter,
    final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
    final RouteBroadcaster routeBroadcaster
  ) {
    return new InMemoryExternalRoutingService(
      eventBus, connectorSettingsSupplier, decryptor, accountSettingsRepository, childAccountPaymentRouter,
      outgoingRoutingTable, routeBroadcaster
    );
  }

  @Bean
  RouteBroadcaster routeBroadcaster(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    @Qualifier(CodecContextConfig.CCP) CodecContext ccpCodecContext,
    AccountSettingsRepository accountSettingsRepository,
    ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
    LinkManager linkManager
  ) {
    return new DefaultRouteBroadcaster(
      connectorSettingsSupplier,
      ccpCodecContext,
      outgoingRoutingTable,
      accountSettingsRepository,
      linkManager
    );
  }

  @Bean
  InterledgerAddressUtils interledgerAddressUtils(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountSettingsRepository accountSettingsRepository
  ) {
    return new InterledgerAddressUtils(connectorSettingsSupplier, accountSettingsRepository);
  }

  @Bean
  PacketRejector packetRejector(final Supplier<ConnectorSettings> connectorSettingsSupplier) {
    return new PacketRejector(() -> connectorSettingsSupplier.get().getOperatorAddress());
  }

  /**
   * A collection of {@link PacketSwitchFilter}. Requests move down through the filter-chain, and responses move back up
   * through the same filter chain in the reverse order, like this:
   *
   * <pre>
   * ┌────────────────────────────────────┐
   * │   AllowedDestinationPacketFilter   │
   * └────△───────────────────────────┬───┘
   * Fulfill/Reject                 Prepare
   * ┌────┴───────────────────────────▽───┐
   * │       MaxPacketAmountFilter        │
   * └────△───────────────────────────┬───┘
   * Fulfill/Reject                 Prepare
   * ┌────┴───────────────────────────▽───┐
   * │         ExpiryPacketFilter         │
   * └────△───────────────────────────┬───┘
   * Fulfill/Reject                 Prepare
   * ┌────┴───────────────────────────▽───┐
   * │       BalanceIlpPacketFilter       │
   * └────△───────────────────────────┬───┘
   * Fulfill/Reject                 Prepare
   * ┌────┴───────────────────────────▽───┐
   * │  ValidateFulfillmentPacketFilter   │
   * └────△───────────────────────────┬───┘
   * Fulfill/Reject                 Prepare
   * ┌────┴───────────────────────────▽───┐
   * │      PeerProtocolPacketFilter      │
   * └────△───────────────────────────┬───┘
   * Fulfill/Reject                 Prepare
   * ┌────┴───────────────────────────▽──┐
   * │                                   │
   * │           PacketSwitch            │
   * │                                   │
   * └───────────────────────────────────┘
   * </pre>
   */
  @Bean
  List<PacketSwitchFilter> packetSwitchFilters(
    RouteBroadcaster routeBroadcaster,
    InterledgerAddressUtils addressUtils,
    BalanceTracker balanceTracker,
    PacketRejector packetRejector,
    SettlementService settlementService,
    @Qualifier(CodecContextConfig.CCP) CodecContext ccpCodecContext, @Qualifier(CodecContextConfig.ILDCP) CodecContext ildcpCodecContext
  ) {
    final ConnectorSettings connectorSettings = connectorSettingsSupplier().get();
    final ImmutableList.Builder<PacketSwitchFilter> filterList = ImmutableList.builder();

    if (connectorSettings.getEnabledFeatures().isRateLimitingEnabled()) {
      filterList.add(new RateLimitIlpPacketFilter(packetRejector));// Limits Data packets...
    }

    filterList.add(
      /////////////////////////////////
      // Incoming Prepare packet Preconditions
      new ExpiryPacketFilter(packetRejector), // Start the expiry timer first to account for delay by other filters
      new AllowedDestinationPacketFilter(packetRejector, addressUtils),
      new MaxPacketAmountFilter(packetRejector),

      // Once the Prepare packet is considered valid, process balance changes.
      new BalanceIlpPacketFilter(packetRejector, balanceTracker),

      //
      new ValidateFulfillmentPacketFilter(packetRejector),

      /////////////////////////////////
      // WARNING: This filter can short-circuit a request, so be careful adding filters after it.
      new PeerProtocolPacketFilter(
        connectorSettingsSupplier(),
        packetRejector,
        routeBroadcaster,
        ccpCodecContext,
        ildcpCodecContext,
        settlementService
      )
    );

    // TODO: Throughput for Money...

    /////////////////////////////////////
    // Non-routable destinations (self.*)
    /////////////////////////////////////

    return filterList.build();
  }

  @Bean
  List<LinkFilter> linkFilters(
    BalanceTracker balanceTracker, SettlementService settlementService
  ) {
    final Supplier<InterledgerAddress> operatorAddressSupplier =
      () -> connectorSettingsSupplier().get().getOperatorAddress().get();

    return Lists.newArrayList(
      //      // TODO: Throughput for Money...
      new OutgoingBalanceLinkFilter(operatorAddressSupplier, balanceTracker, settlementService)
    );
  }

  @Bean
  NextHopPacketMapper nextHopLinkMapper(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    //@Qualifier("externalPaymentRouter")
    ExternalRoutingService externalRoutingService,
    final AccountSettingsRepository accountSettingsRepository,
    final InterledgerAddressUtils addressUtils,
    final JavaMoneyUtils javaMoneyUtils
  ) {
    return new DefaultNextHopPacketMapper(
      connectorSettingsSupplier, externalRoutingService, accountSettingsRepository, addressUtils, javaMoneyUtils
    );
  }

  @Bean
  ConnectorExceptionHandler connectorExceptionHandler(
    Supplier<ConnectorSettings> connectorSettingsSupplier, PacketRejector packetRejector
  ) {
    return new ConnectorExceptionHandler(connectorSettingsSupplier, packetRejector);
  }

  @Bean
  ILPv4PacketSwitch ilpPacketSwitch(
    List<PacketSwitchFilter> packetSwitchFilters, List<LinkFilter> linkFilters,
    LinkManager linkManager, NextHopPacketMapper nextHopPacketMapper,
    ConnectorExceptionHandler connectorExceptionHandler, PacketRejector packetRejector,
    AccountSettingsRepository accountSettingsRepository,
    Cache<AccountId, Optional<? extends AccountSettings>> accountSettingsCache
  ) {
    return new DefaultILPv4PacketSwitch(
      packetSwitchFilters, linkFilters, linkManager, nextHopPacketMapper, connectorExceptionHandler,
      packetRejector, accountSettingsRepository, accountSettingsCache
    );
  }

  @Bean
  ILPv4Connector ilpConnector(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountManager accountManager,
    AccountSettingsRepository accountSettingsRepository,
    LinkManager linkManager,
    ExternalRoutingService externalRoutingService,
    ILPv4PacketSwitch ilpPacketSwitch,
    BalanceTracker balanceTracker,
    EventBus eventBus
  ) {
    return new DefaultILPv4Connector(
      connectorSettingsSupplier,
      accountManager,
      accountSettingsRepository,
      linkManager,
      externalRoutingService,
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
