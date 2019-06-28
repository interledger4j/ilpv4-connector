package org.interledger.ilpv4.connector.balances;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import org.interledger.connector.accounts.AccountId;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.Charset;

import static org.interledger.ilpv4.connector.core.ConfigConstants.FALSE;
import static org.interledger.ilpv4.connector.core.ConfigConstants.ILPV4__CONNECTOR__INMEMORY_BALANCE_TRACKER__ENABLED;
import static org.interledger.ilpv4.connector.core.ConfigConstants.TRUE;

@Configuration
public class RedisBalanceTrackerConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${redis.host:localhost}")
  String redisHost;

  @Value("${redis.port:6379}")
  int redisPort;

  @Value("${redis.password")
  String redisPassword;

  @Autowired
  Decryptor decryptor;

  @Bean
  @ConditionalOnProperty(
    value = ILPV4__CONNECTOR__INMEMORY_BALANCE_TRACKER__ENABLED, havingValue = FALSE, matchIfMissing = true
  )
  BalanceTracker redisBalanceTracker() {
    final BalanceTracker redisBalanceTracker = new RedisBalanceTracker(
      updateBalanceForPrepareScript(), updateBalanceForFulfillScript(), updateBalanceForRejectScript(), redisTemplate()
    );

    try {
      // Try to connect to Redis, but default to InMemoryBalanceTracker if there's no Redis...
      redisBalanceTracker.getBalance(AccountId.of(""));
    } catch (RedisConnectionFailureException e) {
      logger.warn("WARNING: Unable to connect to Redis! Using InMemoryBalanceTracker instead, but this configuration " +
        "should not be used in production. Use RedisBalanceTracker instead!");
      // If debug-output is enabled, then emit the stack-trace.
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }

      return new InMemoryBalanceTracker();
    }

    return redisBalanceTracker;
  }

  /**
   * Used for non-integration tests.
   */
  @Bean
  @ConditionalOnProperty(value = ILPV4__CONNECTOR__INMEMORY_BALANCE_TRACKER__ENABLED, havingValue = TRUE)
  BalanceTracker inMemoryBalanceTracker() {
    return new InMemoryBalanceTracker();
  }

  @Bean
  JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);

    if (redisPassword != null && redisPassword.startsWith(EncryptedSecret.ENCODING_PREFIX)) {
      EncryptedSecret encryptedRedisPassword = EncryptedSecret.fromEncodedValue(redisPassword);
      byte[] decryptedBytes = decryptor.decrypt(encryptedRedisPassword);
      // Use new String for GC.
      config.setPassword(new String(decryptedBytes, Charset.defaultCharset()));
    }

    return new JedisConnectionFactory(config);
  }

  @Bean
  RedisTemplate<String, String> redisTemplate() {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setDefaultSerializer(new StringRedisSerializer());
    template.setEnableDefaultSerializer(true);
    template.setConnectionFactory(jedisConnectionFactory());
    return template;
  }

  @Bean
  RedisScript<Long> updateBalanceForPrepareScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForPrepare.lua"));
    script.setResultType(Long.class);
    return script;
  }

  @Bean
  RedisScript<Long> updateBalanceForFulfillScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForFulfill.lua"));
    script.setResultType(Long.class);
    return script;
  }

  @Bean
  RedisScript<Long> updateBalanceForRejectScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForReject.lua"));
    script.setResultType(Long.class);
    return script;
  }

}
