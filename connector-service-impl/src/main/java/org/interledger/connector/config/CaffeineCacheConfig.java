package org.interledger.connector.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for manually-used caches (e.g., Caffeine caches).
 */
@Configuration
public class CaffeineCacheConfig {

  @Bean
  public Cache<AccountId, Optional<AccountSettings>> accountSettingsCache() {
    return Caffeine.newBuilder()
      .expireAfterAccess(15, TimeUnit.MINUTES) // TODO Make this duration configurable
      .maximumSize(5000) // TODO: Make size configurable.
      .build();
  }

}
