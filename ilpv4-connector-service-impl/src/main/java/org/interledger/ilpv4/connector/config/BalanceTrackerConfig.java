package org.interledger.ilpv4.connector.config;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import org.interledger.ilpv4.connector.balances.InMemoryBalanceTracker;
import org.interledger.ilpv4.connector.balances.RedisBalanceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class BalanceTrackerConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected RedisTemplate<String, String> stringRedisTemplate;

  @Autowired
  protected RedisTemplate<String, ?> jacksonRedisTemplate;

  @Bean
  protected BalanceTracker redisBalanceTracker() {
    try {
      // Try to connect to Redis, but default to InMemoryBalanceTracker if there's no Redis...
      if (stringRedisTemplate.getConnectionFactory().getConnection().ping().equalsIgnoreCase("PONG")) {
        return new RedisBalanceTracker(
          updateBalanceForPrepareScript(), updateBalanceForFulfillScript(), updateBalanceForRejectScript(),
          updateBalanceForIncomingSettlment(), updateBalanceForSettlementRefund(),
          stringRedisTemplate, jacksonRedisTemplate
        );
      }
    } catch (RedisConnectionFailureException e) {
      logger.warn(
        "WARNING: Using InMemoryBalanceTracker. Because this configuration is not durable, it should not be used in " +
          "production deployments. Configure RedisBalanceTracker instead. " +
          "HINT: is Redis running on its configured port, by default 6379?"
      );
      // If debug-output is enabled, then emit the stack-trace.
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
    }
    // Fallback to this always...
    return new InMemoryBalanceTracker();
  }

  @Bean
  protected RedisScript<Long> updateBalanceForPrepareScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForPrepare.lua"));
    script.setResultType(Long.class);
    return script;
  }

  @Bean
  protected RedisScript<List> updateBalanceForFulfillScript() {
    DefaultRedisScript<List> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForFulfill.lua"));
    script.setResultType(List.class);
    return script;
  }

  @Bean
  protected RedisScript<Long> updateBalanceForRejectScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForReject.lua"));
    script.setResultType(Long.class);
    return script;
  }

  @Bean
  protected RedisScript<Long> updateBalanceForSettlementRefund() {
    DefaultRedisScript<Long> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForSettlementRefund.lua"));
    script.setResultType(Long.class);
    return script;
  }

  @Bean
  protected RedisScript<Long> updateBalanceForIncomingSettlment() {
    DefaultRedisScript<Long> script = new DefaultRedisScript();
    script.setLocation(new ClassPathResource("META-INF/scripts/updateBalanceForIncomingSettlement.lua"));
    script.setResultType(Long.class);
    return script;
  }

}