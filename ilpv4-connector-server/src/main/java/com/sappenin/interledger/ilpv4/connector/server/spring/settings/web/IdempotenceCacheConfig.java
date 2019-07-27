package com.sappenin.interledger.ilpv4.connector.server.spring.settings.web;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement.SettlementController;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement.SettlementEngineIdempotencyKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.NamedCacheResolver;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.Arrays;

/**
 * <p>Configuration for Spring Cache using spring-data-cache via @Cacheable annotations, primarily used on the {@link
 * SettlementController}.</p>
 *
 * <p>NOTE: If Redis is not available (i.e., not operating on the configured port), an InMemory cache will be used,
 * but this type of idempotent caching is not suitable for HA environments.</p>
 *
 * @see "https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-caching.html#boot-features-caching"
 */
@Configuration
@EnableCaching
public class IdempotenceCacheConfig extends CachingConfigurerSupport {

  public static final String CACHE_NAME_SETTLEMENTS = "settlements";
  public static final String CACHE_NAME_MESSAGES = "messages";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected Environment environment;

  @Autowired
  JedisConnectionFactory jedisConnectionFactory;

  @Override
  @Bean // important! See parent javadoc
  public CacheManager cacheManager() {

    // Required for @MockMvcTests
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      final SimpleCacheManager cacheManager = new SimpleCacheManager();
      cacheManager.setCaches(Lists.newArrayList(
        new ConcurrentMapCache(CACHE_NAME_SETTLEMENTS),
        new ConcurrentMapCache(CACHE_NAME_MESSAGES)
      ));
      return cacheManager;
    } else {
      // Try to connect using Jedis. If this fails, fallback to an inmemory cache...
      try {
        jedisConnectionFactory.getConnection().ping();
        return RedisCacheManager.create(jedisConnectionFactory);
      } catch (RedisConnectionFailureException e) {
        logger.warn(
          "Unable to communicate with Redis (HINT: Is Redis running on its configured port, by default 6379?). Using an" +
            " in-memory cache instead. Note that the SettlementController requires a Cache for idempotent request " +
            "tracking. If this Connector is running in an HA environment, a distributed cache such as Redis is " +
            "required to avoid duplicate settlement engine requests."
        );
        return new SimpleCacheManager();
      }
    }
  }

  @Override
  @Bean // important! See parent javadoc
  public CacheResolver cacheResolver() {
    // Required for @MockMvcTests
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      return new SimpleCacheResolver(cacheManager());
    } else {
      NamedCacheResolver cacheResolver = new NamedCacheResolver();
      cacheResolver.setCacheManager(cacheManager());
      return cacheResolver;
    }
  }

  /**
   * Allows Spring Cache to determine the caching key based upon the `Idempotency-Key` header that should exist in each
   * HTTP request to the Settlement Engine HTTP endpoints in the Connector.
   */
  @Override
  @Bean // important! See parent javadoc
  public KeyGenerator keyGenerator() {
    return new SettlementEngineIdempotencyKeyGenerator();
  }

  @Override
  @Bean
  public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler();
  }
}
