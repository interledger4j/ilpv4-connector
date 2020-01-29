package org.interledger.connector.server.spring.settings.link;

import org.interledger.connector.accounts.AccountIdResolver;
import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.connector.links.DefaultLinkManager;
import org.interledger.connector.links.DefaultLinkSettingsFactory;
import org.interledger.connector.links.DefaultLinkSettingsValidator;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.links.LinkSettingsValidator;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.server.spring.settings.localFulfillment.LocalSpspFulfillmentConfig;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.link.AbstractStatefulLink.EventBusConnectionEventEmitter;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.LoopbackLink;
import org.interledger.link.LoopbackLinkFactory;
import org.interledger.link.PacketRejector;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.PingLoopbackLinkFactory;
import org.interledger.link.events.LinkConnectionEventEmitter;

import com.google.common.eventbus.EventBus;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Supplier;

/**
 * Baseline configuration for all Links.
 */
@Configuration
@Import( {LocalSpspFulfillmentConfig.class})
public class LinkConfig {

  @Bean
  LoopbackLinkFactory loopbackLinkFactory(PacketRejector packetRejector) {
    return new LoopbackLinkFactory(packetRejector);
  }

  @Bean
  PingLoopbackLinkFactory unidirectionalPingLinkFactory() {
    return new PingLoopbackLinkFactory();
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
  LinkSettingsValidator linkSettingsValidator(
    ConnectorEncryptionService encryptionService, Supplier<ConnectorSettings> connectorSettingsSupplier
  ) {
    return new DefaultLinkSettingsValidator(encryptionService, connectorSettingsSupplier);
  }

  @Bean
  LinkConnectionEventEmitter linkEventEmitter(EventBus eventBus) {
    return new EventBusConnectionEventEmitter(eventBus);
  }

  @Bean
  LinkManager linkManager(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    EventBus eventBus,
    AccountSettingsRepository accountSettingsRepository,
    LinkSettingsFactory linkSettingsFactory,
    LinkFactoryProvider linkFactoryProvider,
    AccountIdResolver accountIdResolver,
    CircuitBreakerConfig circuitBreakerConfig
  ) {
    return new DefaultLinkManager(
      () -> connectorSettingsSupplier.get().operatorAddress(),
      accountSettingsRepository,
      linkSettingsFactory,
      linkFactoryProvider,
      accountIdResolver,
      circuitBreakerConfig,
      eventBus
    );
  }
}
