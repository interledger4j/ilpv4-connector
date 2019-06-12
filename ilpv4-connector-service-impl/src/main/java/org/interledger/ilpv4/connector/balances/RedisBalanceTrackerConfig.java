package org.interledger.ilpv4.connector.balances;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import org.interledger.connector.accounts.AccountId;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.Charset;

@Configuration
//@ConditionalOnProperty(name = "spring.data.redis.repositories.enabled", havingValue = "true")
public class RedisBalanceTrackerConfig {

  @Value("${redis.host:localhost}")
  String redisHost;

  @Value("${redis.port:6379}")
  int redisPort;

  @Value("${redis.password")
  String redisPassword;

  @Autowired
  Decryptor decryptor;

  @Bean
  BalanceTracker redisBalanceTracker() {
    final BalanceTracker balanceTracker = new RedisBalanceTracker(updateBalanceForPrepareScript(),
      updateBalanceForFulfillScript(),
      updateBalanceForRejectScript(), redisTemplate());

    try {
      // Try to connect to Redis...
      balanceTracker.getBalance(AccountId.of(""));
    } catch (Exception e) {
      throw new RuntimeException("Unable to connect to Redis. Redis is required to operate the Connector!", e);
    }

    return balanceTracker;
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