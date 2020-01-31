package org.interledger.connector.config;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.packetswitch.filters.RateLimitIlpPacketFilter;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.money.convert.ConversionQuery;
import javax.money.convert.ExchangeRate;

/**
 * Configuration for manually-used caches (e.g., Caffeine caches).
 */
@Configuration
@SuppressWarnings("UnstableApiUsage")
public class CaffeineCacheConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Bean
  public CacheMetricsCollector cacheMetricsCollector() {
    final CacheMetricsCollector cacheMetricsCollector = new CacheMetricsCollector();

    // If running in a topology, the default registration will throw an error because the Collectors have already
    // been registered, and there's only a single default registry per-JVM. Thus, we can ignore the exception and
    // continue along as if the call was successful (Note that the functionality is only used in Topologies where
    // multiple Connectors run in parallel in the same JVM.
    try {
      cacheMetricsCollector.register();
    } catch (IllegalArgumentException e) {
      // This error is benign - it only manifests when more than one Connector are run in the same JVM.
      logger.debug(e.getMessage(), e);
    }
    return cacheMetricsCollector;
  }

  @Bean
  public Cache<AccountId, Optional<AccountSettings>> accountSettingsCache(CacheMetricsCollector cacheMetricsCollector) {
    final Cache<AccountId, Optional<AccountSettings>> accountSettingsCache = Caffeine.newBuilder()
        .recordStats() // Publish stats to Prometheus.
        .expireAfterAccess(15, TimeUnit.MINUTES) // TODO Make this duration configurable
        .maximumSize(5000) // TODO: Make size configurable.
        .build();

    cacheMetricsCollector.addCache("accountSettingsCache", accountSettingsCache);
    return accountSettingsCache;
  }

  @Bean
  public AccountSettingsLoadingCache accountSettingsLoadingCache(
      AccountSettingsRepository accountSettingsRepository,
      Cache<AccountId, Optional<AccountSettings>> accountSettingsCache
  ) {
    return new AccountSettingsLoadingCache(
        accountSettingsRepository,
        // NOTE: No need to enable Prometheus here because it is enabled for this cache inside of
        // `accountSettingsCache()`
        accountSettingsCache
    );
  }

  /**
   * Cache used for rate-limiting inside of {@link RateLimitIlpPacketFilter}.
   *
   * @return A {@link Cache}.
   */
  @Bean
  public Cache<AccountId, Optional<RateLimiter>> rateLimiterCache(CacheMetricsCollector cacheMetricsCollector) {
    final Cache<AccountId, Optional<RateLimiter>> rateLimiterCache = Caffeine.newBuilder()
        .recordStats() // Publish stats to prometheus
        //.maximumSize(100) // Not enabled for now in order to support many accounts.
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .build(); // No default loading function.

    cacheMetricsCollector.addCache("rateLimiterCache", rateLimiterCache);

    return rateLimiterCache;
  }

  /**
   * Cache used for rate-limiting inside of {@link RateLimitIlpPacketFilter}.
   *
   * @return A {@link Cache}.
   */
  @Bean
  public Cache<ConversionQuery, ExchangeRate> fxCache(CacheMetricsCollector cacheMetricsCollector,
                                                      @Value("${interledger.connector.cache.fxTtl:30}") long fxCacheTimeout) {
    final Cache<ConversionQuery, ExchangeRate> fxCache = Caffeine.newBuilder()
        .recordStats() // Publish stats to prometheus
        .expireAfterAccess(fxCacheTimeout, TimeUnit.SECONDS)
        .build(); // No default loading function.

    cacheMetricsCollector.addCache("fxCache", fxCache);

    return fxCache;
  }
}
