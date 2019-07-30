package org.interledger.ilpv4.connector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.Charset;

@Configuration
public class RedisConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${redis.host:localhost}")
  protected String redisHost;

  // Technically, this _could_ be typed as an int, but the @MockWebMvc doesn't properly setup the AnnotationProcessor
  // that Spring Boot uses for parsing SPEL, so instead this is typed as a String and value-checked below where used.
  @Value("${redis.port:6379}")
  protected String redisPort = "6379";

  @Value("${redis.password")
  protected String redisPassword;

  @Autowired
  protected Decryptor decryptor;

  @Autowired
  protected ObjectMapper objectMapper;

  @Bean
  protected JedisConnectionFactory jedisConnectionFactory() {

    int actualRedisPort;
    try {
      actualRedisPort = Integer.parseInt(redisPort);
    } catch (Exception e) {
      actualRedisPort = 6379;
    }
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, actualRedisPort);

    if (redisPassword != null && redisPassword.startsWith(EncryptedSecret.ENCODING_PREFIX)) {
      EncryptedSecret encryptedRedisPassword = EncryptedSecret.fromEncodedValue(redisPassword);
      byte[] decryptedBytes = decryptor.decrypt(encryptedRedisPassword);
      // Use new String for GC.
      config.setPassword(new String(decryptedBytes, Charset.defaultCharset()));
    }

    final JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(config);
    jedisConnectionFactory.getPoolConfig().setMaxTotal(25); // TODO: Make this configurable
    jedisConnectionFactory.getPoolConfig().setMinIdle(1); // TODO: Make this configurable
    jedisConnectionFactory.getPoolConfig().setMaxIdle(2); // TODO: Make this configurable

    try {
      // Try to connect to Redis, but default to InMemoryBalanceTracker if there's no Redis...
      if (!jedisConnectionFactory.getConnection().ping().equalsIgnoreCase("PONG")) {
        logger.warn("WARNING: Unable to connect to Redis.");
      }
    } catch (RedisConnectionFailureException e) {
      logger.warn("WARNING: Unable to connect to Redis!");
      // If debug-output is enabled, then emit the stack-trace.
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
    }
    // Even if unconnected, we return anyway because implementations that depend on this factory will detect this
    // condition and fallback to in-memory implementations.
    return jedisConnectionFactory;
  }

  @Bean
  protected RedisTemplate<String, ?> jacksonRedisTemplate() {
    final RedisTemplate<String, ?> template = new RedisTemplate<>();

    template.setDefaultSerializer(new StringRedisSerializer());
    template.setEnableDefaultSerializer(true);
    template.setConnectionFactory(jedisConnectionFactory());

    return template;
  }
}
