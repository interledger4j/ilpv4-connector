package org.interledger.connector.config;

import org.interledger.connector.balances.AccountBalanceService;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.InMemoryBalanceTracker;
import org.interledger.connector.balances.RedisBalanceTracker;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class BalanceTrackerConfig {

  public static final String BALANCE_TRACKING = "BALANCE_TRACKING";

  public static final String BALANCE_TRACKING_JACKSON_REDIS_TEMPLATE_BEAN_NAME = "balanceTrackingJacksonRedisTemplate";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected Environment environment;

  @Autowired
  protected LettuceConnectionFactory lettuceConnectionFactory;

  @Bean(BALANCE_TRACKING_JACKSON_REDIS_TEMPLATE_BEAN_NAME)
  @Qualifier(BALANCE_TRACKING)
  protected RedisTemplate<String, ?> balanceTrackingRedisTemplate() {
    final RedisTemplate<String, ?> template = new RedisTemplate<>();

    template.setEnableDefaultSerializer(true);
    template.setDefaultSerializer(new StringRedisSerializer());
    template.setConnectionFactory(lettuceConnectionFactory);

    return template;
  }

  @Bean
  protected BalanceTracker redisBalanceTracker(
    @Qualifier(BALANCE_TRACKING) RedisTemplate<String, String> stringRedisTemplate,
    @Qualifier(BALANCE_TRACKING) RedisTemplate<String, ?> jacksonRedisTemplate
  ) {
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
      logger.warn("\n#############################\n"
        + "WARNING: Using InMemoryBalanceTracker. Because this configuration is not durable, it should not be used "
        + "in production deployments. Configure RedisBalanceTracker instead. HINT: is Redis running on its "
        + "configured port, by default 6379?\n"
        + "#############################"
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

  @Bean
  protected AccountBalanceService accountBalanceService(BalanceTracker balanceTracker,
    AccountSettingsRepository accountSettingsRepository) {
    return new AccountBalanceService(balanceTracker, accountSettingsRepository);
  }

}
