package org.interledger.connector.server.spring.settings;

import static org.interledger.connector.accounts.sub.LocalDestinationAddressUtils.PING_ACCOUNT_ID;

import org.interledger.connector.ConnectorExceptionHandler;
import org.interledger.connector.DefaultILPv4Connector;
import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.accounts.AccessTokenManager;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountIdResolver;
import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.AccountSettingsCache;
import org.interledger.connector.accounts.AccountSettingsResolver;
import org.interledger.connector.accounts.BtpAccountIdResolver;
import org.interledger.connector.accounts.DefaultAccessTokenManager;
import org.interledger.connector.accounts.DefaultAccountIdResolver;
import org.interledger.connector.accounts.DefaultAccountManager;
import org.interledger.connector.accounts.DefaultAccountSettingsResolver;
import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.config.BalanceTrackerConfig;
import org.interledger.connector.config.CaffeineCacheConfig;
import org.interledger.connector.config.RedisConfig;
import org.interledger.connector.config.SettlementConfig;
import org.interledger.connector.config.SpspClientConfig;
import org.interledger.connector.config.SpspReceiverConfig;
import org.interledger.connector.config.XrplScanningConfig;
import org.interledger.connector.events.DefaultPacketEventPublisher;
import org.interledger.connector.events.PacketEventPublisher;
import org.interledger.connector.fx.JavaMoneyUtils;
import org.interledger.connector.fxrates.DefaultFxRateOverridesManager;
import org.interledger.connector.fxrates.FxRateOverridesManager;
import org.interledger.connector.links.DefaultIldcpFetcherFactory;
import org.interledger.connector.links.DefaultNextHopPacketMapper;
import org.interledger.connector.links.IldcpFetcherFactory;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.links.LinkSettingsValidator;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.connector.links.filters.OutgoingBalanceLinkFilter;
import org.interledger.connector.links.filters.OutgoingMaxPacketAmountLinkFilter;
import org.interledger.connector.links.filters.OutgoingMetricsLinkFilter;
import org.interledger.connector.links.filters.OutgoingStreamPaymentLinkFilter;
import org.interledger.connector.localsend.LocalPacketSwitchLinkFactory;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.connector.packetswitch.DefaultILPv4PacketSwitch;
import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.connector.packetswitch.InterledgerAddressUtils;
import org.interledger.connector.packetswitch.filters.AllowedDestinationPacketFilter;
import org.interledger.connector.packetswitch.filters.BalanceIlpPacketFilter;
import org.interledger.connector.packetswitch.filters.ExpiryPacketFilter;
import org.interledger.connector.packetswitch.filters.MaxPacketAmountFilter;
import org.interledger.connector.packetswitch.filters.PacketMetricsFilter;
import org.interledger.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.connector.packetswitch.filters.PeerProtocolPacketFilter;
import org.interledger.connector.packetswitch.filters.RateLimitIlpPacketFilter;
import org.interledger.connector.packetswitch.filters.StreamPaymentIlpPacketFilter;
import org.interledger.connector.packetswitch.filters.ValidateFulfillmentPacketFilter;
import org.interledger.connector.payments.DefaultSendPaymentService;
import org.interledger.connector.payments.FulfillmentGeneratedEventAggregator;
import org.interledger.connector.payments.FulfillmentGeneratedEventConverter;
import org.interledger.connector.payments.InDatabaseStreamPaymentManager;
import org.interledger.connector.payments.InMemoryStreamPaymentManager;
import org.interledger.connector.payments.SendPaymentService;
import org.interledger.connector.payments.SimpleExchangeRateCalculator;
import org.interledger.connector.payments.SimpleStreamSenderFactory;
import org.interledger.connector.payments.StreamPaymentFromEntityConverter;
import org.interledger.connector.payments.StreamPaymentManager;
import org.interledger.connector.payments.StreamPaymentToEntityConverter;
import org.interledger.connector.payments.StreamSenderFactory;
import org.interledger.connector.payments.SynchronousFulfillmentGeneratedEventAggregator;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.repositories.AccessTokensRepository;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.DeletedAccountSettingsRepository;
import org.interledger.connector.persistence.repositories.FxRateOverridesRepository;
import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.persistence.repositories.StreamPaymentsRepository;
import org.interledger.connector.pubsub.RedisPubSubConfig;
import org.interledger.connector.routing.DefaultRouteBroadcaster;
import org.interledger.connector.routing.ExternalRoutingService;
import org.interledger.connector.routing.ForwardingRoutingTable;
import org.interledger.connector.routing.InMemoryExternalRoutingService;
import org.interledger.connector.routing.InMemoryForwardingRoutingTable;
import org.interledger.connector.routing.InMemoryRoutingTable;
import org.interledger.connector.routing.LocalDestinationAddressPaymentRouter;
import org.interledger.connector.routing.RouteBroadcaster;
import org.interledger.connector.routing.RouteUpdate;
import org.interledger.connector.routing.StaticRoutesManager;
import org.interledger.connector.server.spring.gcp.GcpPubSubConfig;
import org.interledger.connector.server.spring.settings.crypto.CryptoConfig;
import org.interledger.connector.server.spring.settings.javamoney.JavaMoneyConfig;
import org.interledger.connector.server.spring.settings.link.LinkConfig;
import org.interledger.connector.server.spring.settings.metrics.MetricsConfiguration;
import org.interledger.connector.server.spring.settings.web.SpringConnectorWebMvc;
import org.interledger.connector.server.wallet.spring.config.WalletConfig;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.properties.ConnectorSettingsFromPropertyFile;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.CryptoKeys;
import org.interledger.crypto.Decryptor;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.PacketRejector;
import org.interledger.link.PingLoopbackLink;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.sender.StreamConnectionManager;

import ch.qos.logback.classic.LoggerContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

/**
 * <p>Primary configuration for the Connector.</p>
 *
 * <p>See the package-info in {@link org.interledger.connector.server.spring.settings} for more
 * details.</p>
 */
@SuppressWarnings("UnstableApiUsage")
@Configuration
@EnableConfigurationProperties( {ConnectorSettingsFromPropertyFile.class})
@Import( {
  JavaMoneyConfig.class,
  CodecContextConfig.class,
  ConnectorPersistenceConfig.class,
  CryptoConfig.class,
  ResiliencyConfig.class,
  CaffeineCacheConfig.class,
  RedisConfig.class,
  SpspReceiverConfig.class,
  SpspClientConfig.class,
  SettlementConfig.class,
  BalanceTrackerConfig.class,
  LinkConfig.class,
  MetricsConfiguration.class,
  SpringConnectorWebMvc.class,
  WalletConfig.class,
  GcpPubSubConfig.class,
  RedisPubSubConfig.class,
  XrplScanningConfig.class,
  SpringAsyncConfig.class,
})
// support extension by looking for annotated Component/Config classes under the configured extensions.basePackage
@ComponentScan(basePackages = "${interledger.connector.extensions.basePackage:org.interledger.connector.extensions}",
  useDefaultFilters = false,
  includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Component.class)
)
public class SpringConnectorConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  @Lazy
  private ExternalRoutingService externalRoutingService;

  @Autowired
  private DeletedAccountSettingsRepository deletedAccountSettingsRepository;

  @Autowired
  private Environment env;

  @Autowired
  private BuildProperties buildProperties;

  @Autowired
  private Supplier<ConnectorSettings> connectorSettingsSupplier;

  @Value("${interledger.connector.enabledFeatures.localSpspFulfillmentEnabled:false}")
  private boolean localSpspFulfillmentEnabled;

  @Value("${interledger.connector.spsp.addressPrefixSegment:spsp}")
  private String spspAddressPrefixSegment;

  @Value("${interledger.connector.globalRoutingSettings.localAccountsAddressSegment:accounts}")
  private String localAccountsAddressPrefixSegment;

  /**
   * <p>Initialize the connector after constructing it.</p>
   */
  @PostConstruct
  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private void init() {
    // Configure Sentry
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.putProperty("node-ilp-address", connectorSettingsSupplier.get().operatorAddress().getValue());
    context.putProperty("name", buildProperties.getName());
    context.putProperty("group", buildProperties.getGroup());
    context.putProperty("artifact", buildProperties.getArtifact());
    context.putProperty("release", buildProperties.getVersion());
    context.putProperty("spring-profiles", Arrays.stream(env.getActiveProfiles()).collect(Collectors.joining(" ")));
  }

  /**
   * All internal Connector events propagate locally in this JVM using this EventBus.
   */
  @Bean
  EventBus eventBus() {
    final ThreadFactory factory = new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat("connector-event-subsystem-%d")
      .build();
    final ExecutorService threadPool = Executors.newFixedThreadPool(30, factory);

    return new AsyncEventBus("connector-event-subsystem", threadPool);
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
  AccountIdResolver accountIdResolver(BtpAccountIdResolver btpAccountIdResolver) {
    return btpAccountIdResolver;
  }

  @Bean
  BtpAccountIdResolver btpAccountIdResolver() {
    return new DefaultAccountIdResolver();
  }

  @Bean
  IldcpFetcherFactory ildcpFetcherFactory() {
    return new DefaultIldcpFetcherFactory();
  }

  @Bean
  AccountManager accountManager(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountSettingsRepository accountSettingsRepository,
    LinkManager linkManager,
    ConversionService conversionService,
    SettlementEngineClient settlementEngineClient,
    LinkSettingsFactory linkSettingsFactory,
    LinkSettingsValidator linkSettingsValidator,
    IldcpFetcherFactory ildcpFetcherFactory
  ) {
    return new DefaultAccountManager(
      connectorSettingsSupplier, conversionService, accountSettingsRepository, deletedAccountSettingsRepository,
      linkManager, settlementEngineClient,
      linkSettingsFactory,
      linkSettingsValidator,
      ildcpFetcherFactory,
      eventBus());
  }

  @Bean
  AccessTokenManager accessTokenManager(PasswordEncoder passwordEncoder,
    AccessTokensRepository accessTokensRepository,
    EventBus eventBus) {
    return new DefaultAccessTokenManager(passwordEncoder, accessTokensRepository, eventBus);
  }

  @Bean
  FxRateOverridesManager fxRateOverridesManager(FxRateOverridesRepository respository) {
    return new DefaultFxRateOverridesManager(respository);
  }

  @Bean
  StaticRoutesManager staticRoutesManager() {
    return externalRoutingService;
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
  LocalDestinationAddressPaymentRouter childAccountPaymentRouter(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountSettingsRepository accountSettingsRepository,
    final LocalDestinationAddressUtils localDestinationAddressUtils
  ) {

    // If the Ping Protocol is enabled, we need to ensure that there is a Ping account suitable to accept value for
    // Ping requests.
    if (connectorSettingsSupplier.get().enabledProtocols().isPingProtocolEnabled() &&
      !accountSettingsRepository.findByAccountId(PING_ACCOUNT_ID).isPresent()) {
      // Create this account.

      final AccountSettings pingAccountSettings = AccountSettings.builder()
        .accountId(PING_ACCOUNT_ID)
        .accountRelationship(AccountRelationship.CHILD)
        .assetCode("USD") // TODO: Make this configurable, or else the same as the Connector's base currency.
        // This is set very high so that it can not lose very small sums of money, which are likely for PINGs. At
        // current pricing, this supports XRP pings at a scale of 9 using 1 unit.
        .assetScale(11) // TODO: Make this configurable, or else the same as the Connector's base currency.
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

    return new LocalDestinationAddressPaymentRouter(connectorSettingsSupplier, localDestinationAddressUtils);
  }

  @Bean
  ExternalRoutingService externalRoutingService(
    final EventBus eventBus,
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final Decryptor decryptor,
    final AccountSettingsRepository accountSettingsRepository,
    final StaticRoutesRepository staticRoutesRepository,
    final LocalDestinationAddressPaymentRouter localDestinationAddressPaymentRouter,
    final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
    final RouteBroadcaster routeBroadcaster
  ) {
    return new InMemoryExternalRoutingService(
      eventBus,
      connectorSettingsSupplier,
      decryptor,
      accountSettingsRepository,
      staticRoutesRepository,
      localDestinationAddressPaymentRouter,
      new InMemoryRoutingTable(),
      outgoingRoutingTable,
      routeBroadcaster
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
    return new PacketRejector(() -> connectorSettingsSupplier.get().operatorAddress());
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
    MetricsService metricsService,
    @Qualifier(CodecContextConfig.CCP) CodecContext ccpCodecContext,
    @Qualifier(CodecContextConfig.ILDCP) CodecContext ildcpCodecContext,
    Cache<AccountId, Optional<RateLimiter>> rateLimiterCache,
    FulfillmentGeneratedEventAggregator fulfillmentGeneratedEventAggregator) {
    final ConnectorSettings connectorSettings = connectorSettingsSupplier().get();
    final ImmutableList.Builder<PacketSwitchFilter> filterList = ImmutableList.builder();

    // This goes first so that it counts all fulfill/reject packets.
    filterList.add(new PacketMetricsFilter(packetRejector, metricsService));

    if (connectorSettings.enabledFeatures().isRateLimitingEnabled()) {
      filterList.add(
        new RateLimitIlpPacketFilter(packetRejector, metricsService, rateLimiterCache));// Limits Data packets...
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
      ),
      new StreamPaymentIlpPacketFilter(packetRejector, fulfillmentGeneratedEventAggregator)
    );

    // TODO: Throughput for Money...

    /////////////////////////////////////
    // Non-routable destinations (self.*)
    /////////////////////////////////////

    return filterList.build();
  }

  @Bean
  List<LinkFilter> linkFilters(
    BalanceTracker balanceTracker, SettlementService settlementService, MetricsService metricsService,
    EventBus eventBus,
    FulfillmentGeneratedEventAggregator fulfillmentGeneratedEventAggregator) {
    final Supplier<InterledgerAddress> operatorAddressSupplier =
      () -> connectorSettingsSupplier().get().operatorAddress();

    return Lists.newArrayList(
      // TODO: Throughput for Money...
      new OutgoingMetricsLinkFilter(operatorAddressSupplier, metricsService),
      new OutgoingMaxPacketAmountLinkFilter(operatorAddressSupplier),
      new OutgoingBalanceLinkFilter(operatorAddressSupplier, balanceTracker, settlementService, eventBus),
      new OutgoingStreamPaymentLinkFilter(operatorAddressSupplier, fulfillmentGeneratedEventAggregator)
    );
  }

  @Bean
  NextHopPacketMapper nextHopLinkMapper(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    ExternalRoutingService externalRoutingService,
    InterledgerAddressUtils addressUtils,
    JavaMoneyUtils javaMoneyUtils,
    AccountSettingsLoadingCache accountSettingsLoadingCache
  ) {
    return new DefaultNextHopPacketMapper(
      connectorSettingsSupplier, externalRoutingService, addressUtils, javaMoneyUtils, accountSettingsLoadingCache
    );
  }

  @Bean
  PacketEventPublisher packetEventPublisher(EventBus eventBus) {
    return new DefaultPacketEventPublisher(eventBus);
  }

  @Bean
  ConnectorExceptionHandler connectorExceptionHandler(
    Supplier<ConnectorSettings> connectorSettingsSupplier, PacketRejector packetRejector,
    PacketEventPublisher packetEventPublisher) {
    return new ConnectorExceptionHandler(connectorSettingsSupplier, packetRejector, packetEventPublisher);
  }

  @Bean
  ILPv4PacketSwitch ilpPacketSwitch(
    List<PacketSwitchFilter> packetSwitchFilters,
    List<LinkFilter> linkFilters,
    LinkManager linkManager,
    NextHopPacketMapper nextHopPacketMapper,
    ConnectorExceptionHandler connectorExceptionHandler,
    PacketRejector packetRejector,
    AccountSettingsLoadingCache accountSettingsLoadingCache,
    PacketEventPublisher packetEventPublisher,
    LocalDestinationAddressUtils localDestinationAddressUtils
  ) {
    return new DefaultILPv4PacketSwitch(
      packetSwitchFilters,
      linkFilters,
      linkManager,
      nextHopPacketMapper,
      connectorExceptionHandler,
      packetRejector,
      accountSettingsLoadingCache,
      packetEventPublisher,
      localDestinationAddressUtils
    );
  }

  @Bean
  ILPv4Connector ilpConnector(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    AccountManager accountManager,
    AccountSettingsRepository accountSettingsRepository,
    FxRateOverridesRepository fxRateOverridesRepository,
    LinkManager linkManager,
    ExternalRoutingService externalRoutingService,
    ILPv4PacketSwitch ilpPacketSwitch,
    BalanceTracker balanceTracker,
    EventBus eventBus,
    SettlementService settlementService,
    AccountSettingsCache accountSettingsCache
  ) {
    return new DefaultILPv4Connector(
      connectorSettingsSupplier,
      accountManager,
      accountSettingsRepository,
      fxRateOverridesRepository,
      linkManager,
      externalRoutingService,
      ilpPacketSwitch,
      balanceTracker,
      settlementService,
      accountSettingsCache,
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

  @Bean
  CryptoKeys connectorKeys(Supplier<ConnectorSettings> connectorSettingsSupplier) {
    return connectorSettingsSupplier.get().keys();
  }

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  LocalDestinationAddressUtils localDestinationAddressUtils(Supplier<ConnectorSettings> connectorSettingsSupplier) {
    return new LocalDestinationAddressUtils() {
      @Override
      public Supplier<InterledgerAddress> getConnectorOperatorAddress() {
        return () -> connectorSettingsSupplier.get().operatorAddress();
      }

      @Override
      public boolean isLocalSpspFulfillmentEnabled() {
        return localSpspFulfillmentEnabled && !spspAddressPrefixSegment.isEmpty();
      }

      @Override
      public String getSpspAddressPrefixSegment() {
        return spspAddressPrefixSegment;
      }

      @Override
      public String getLocalAccountsAddressPrefixSegment() {
        return localAccountsAddressPrefixSegment;
      }
    };
  }

  @Bean
  protected StreamPaymentManager streamPaymentManager(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    StreamPaymentsRepository streamPaymentsRepository,
    EventBus eventBus
  ) {
    switch (connectorSettingsSupplier.get().enabledFeatures().streamPaymentAggregationMode()) {
      case IN_POSTGRES:
        return new InDatabaseStreamPaymentManager(
          streamPaymentsRepository,
          new StreamPaymentFromEntityConverter(),
          new StreamPaymentToEntityConverter(),
          eventBus);
      default:
        return new InMemoryStreamPaymentManager(eventBus);
    }
  }

  @Bean
  protected FulfillmentGeneratedEventAggregator fulfilledTransactionAggregator(
    StreamPaymentManager streamPaymentManager,
    StreamEncryptionService streamEncryptionService,
    CodecContext streamCodecContext
  ) {
    return new SynchronousFulfillmentGeneratedEventAggregator(streamPaymentManager,
      new FulfillmentGeneratedEventConverter(streamEncryptionService, streamCodecContext));
  }

  @Bean
  protected StreamSenderFactory streamSenderFactory(StreamEncryptionService streamEncryptionService) {
    return new SimpleStreamSenderFactory(streamEncryptionService, new StreamConnectionManager());
  }

  @Bean
  protected ExchangeRateCalculator exchangeRateCalculator() {
    return new SimpleExchangeRateCalculator();
  }

  @Bean
  LocalPacketSwitchLinkFactory localPacketSwitchLinkFactory(ILPv4PacketSwitch ilpPacketSwitch) {
    return new LocalPacketSwitchLinkFactory(ilpPacketSwitch);
  }

  @Bean
  protected StreamEncryptionService streamEncryptionService() {
    return new JavaxStreamEncryptionService();
  }

  @Bean
  protected SendPaymentService streamPaymentService(StreamSenderFactory streamSenderFactory,
    SpspClient spspClient,
    ExchangeRateCalculator exchangeRateCalculator,
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    StreamPaymentManager streamPaymentManager,
    AccountManager accountManager,
    LocalPacketSwitchLinkFactory localPacketSwitchLinkFactory) {
    return new DefaultSendPaymentService(
      streamSenderFactory,
      spspClient,
      exchangeRateCalculator,
      () -> connectorSettingsSupplier.get().operatorAddress(),
      streamPaymentManager,
      accountManager,
      localPacketSwitchLinkFactory,
      20 // FIXME add to configuration
    );
  }

}
