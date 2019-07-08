package org.interledger.ilpv4.connector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sappenin.interledger.ilpv4.connector.settlement.HttpResponseInfo;
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
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.Charset;
import java.util.UUID;

@Configuration
public class RedisConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${redis.host:localhost}")
  protected String redisHost;

  @Value("${redis.port:6379}")
  protected int redisPort;

  @Value("${redis.password")
  protected String redisPassword;

  @Autowired
  protected Decryptor decryptor;

  @Autowired
  protected ObjectMapper objectMapper;

  @Bean
  protected JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);

    if (redisPassword != null && redisPassword.startsWith(EncryptedSecret.ENCODING_PREFIX)) {
      EncryptedSecret encryptedRedisPassword = EncryptedSecret.fromEncodedValue(redisPassword);
      byte[] decryptedBytes = decryptor.decrypt(encryptedRedisPassword);
      // Use new String for GC.
      config.setPassword(new String(decryptedBytes, Charset.defaultCharset()));
    }

    JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(config);

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

  // WARNING: Must be named `stringRedisTemplate` in order to supercede the default Spring AutoConfig.
  @Bean
  protected RedisTemplate<String, String> redisTemplate() {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setDefaultSerializer(new StringRedisSerializer());
    template.setEnableDefaultSerializer(true);
    template.setConnectionFactory(jedisConnectionFactory());

    return template;
  }

  @Bean
  protected RedisTemplate<UUID, HttpResponseInfo> idempotencRedisTemplate() {
    RedisTemplate<UUID, HttpResponseInfo> template = new RedisTemplate<>();

    Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(HttpResponseInfo.class);
    serializer.setObjectMapper(objectMapper);
    template.setDefaultSerializer(serializer);
    template.setEnableDefaultSerializer(true);
    template.setConnectionFactory(jedisConnectionFactory());

    return template;
  }

}
