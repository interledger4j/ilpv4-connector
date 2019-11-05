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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for manually-used caches (e.g., Caffeine caches).
 */
@Configuration
public class CaffeineCacheConfig {

  // Uses the default registry...
  private final CacheMetricsCollector cacheMetrics = new CacheMetricsCollector().register();

  @Bean
  public Cache<AccountId, Optional<AccountSettings>> accountSettingsCache() {
    final Cache<AccountId, Optional<AccountSettings>> accountSettingsCache = Caffeine.newBuilder()
        .recordStats() // Publish stats to Prometheus.
        .expireAfterAccess(15, TimeUnit.MINUTES) // TODO Make this duration configurable
        .maximumSize(5000) // TODO: Make size configurable.
        .build();

    cacheMetrics.addCache("accountSettingsCache", accountSettingsCache);
    return accountSettingsCache;
  }

  @Bean
  public AccountSettingsLoadingCache accountSettingsLoadingCache(AccountSettingsRepository accountSettingsRepository) {
    return new AccountSettingsLoadingCache(
        accountSettingsRepository,
        // NOTE: No need to enable Prometheus here because it is enabled for this cache inside of
        // `accountSettingsCache()`
        accountSettingsCache()
    );
  }

  /**
   * Cache used for rate-limiting inside of {@link RateLimitIlpPacketFilter}.
   *
   * @return
   */
  @Bean
  public Cache<AccountId, Optional<RateLimiter>> rateLimiterCache() {
    final Cache<AccountId, Optional<RateLimiter>> rateLimiterCache = Caffeine.newBuilder()
        .recordStats() // Publish stats to prometheus
        //.maximumSize(100) // Not enabled for now in order to support many accounts.
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .build(); // No default loading function.

    cacheMetrics.addCache("rateLimiterCache", rateLimiterCache);

    return rateLimiterCache;
  }
}
