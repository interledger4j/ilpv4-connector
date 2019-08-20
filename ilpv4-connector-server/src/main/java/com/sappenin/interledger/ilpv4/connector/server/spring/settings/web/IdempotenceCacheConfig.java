package com.sappenin.interledger.ilpv4.connector.server.spring.settings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

/**
 * <p>Configuration for Spring Cache using spring-data-cache via @Cacheable annotations, primarily used on the
 * {@link SettlementController}.</p>
 *
 * <p>NOTE: If Redis is not available (i.e., not operating on the configured port), an InMemory cache will be used,
 * but this type of idempotent caching is not suitable for HA environments.</p>
 *
 * @see "https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-caching.html#boot-features-caching"
 */
@Configuration
@EnableCaching
public class IdempotenceCacheConfig extends CachingConfigurerSupport {
  // Used to store Idempotent ResponseEntity data for `/settlements` requests...
  // NOTE: This is both the cache name (in the JVM) as well as the prefix for the Redis key.
  public static final String SETTLEMENT_IDEMPOTENCE = "settlement_idempotence";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected Environment environment;

  @Autowired
  protected JedisConnectionFactory jedisConnectionFactory;

  @Autowired
  protected ObjectMapper objectMapper;

  @Override
  @Bean // important! See parent javadoc
  public CacheManager cacheManager() {

    // Required for @MockMvcTests
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      final SimpleCacheManager cacheManager = new SimpleCacheManager();
      cacheManager.setCaches(Lists.newArrayList(
        new ConcurrentMapCache(SETTLEMENT_IDEMPOTENCE)
      ));
      return cacheManager;
    } else {
      // Try to connect using Jedis. If this fails, fallback to an in-memory cache...
      try {
        jedisConnectionFactory.getConnection().ping();

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
          .prefixKeysWith(SETTLEMENT_IDEMPOTENCE + ":")
          .entryTtl(Duration.ofMinutes(5)) // TODO: Make configurable.
          .disableCachingNullValues()
          .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
          )
          // Required or else the JdkSerializer will be used, which throws SerializationFailedException because
          // ResponseEntity is not Serializable.
          .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper))
          );

        Map<String, RedisCacheConfiguration> initialDefaultConfigurations = Maps.newHashMap();
        initialDefaultConfigurations.put(SETTLEMENT_IDEMPOTENCE, defaultCacheConfig);

        return RedisCacheManager.builder(jedisConnectionFactory)
          .initialCacheNames(Sets.newHashSet(SETTLEMENT_IDEMPOTENCE))
          .cacheDefaults(defaultCacheConfig)
          .withInitialCacheConfigurations(initialDefaultConfigurations)
          .build();
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
      cacheResolver.setCacheNames(Lists.newArrayList(SETTLEMENT_IDEMPOTENCE));
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
  @Bean // important! See parent javadoc
  public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler();
  }
}
