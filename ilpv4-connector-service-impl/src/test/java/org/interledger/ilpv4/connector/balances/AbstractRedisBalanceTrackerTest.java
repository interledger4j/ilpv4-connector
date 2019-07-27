package org.interledger.ilpv4.connector.balances;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.connector.accounts.AccountId;
import org.interledger.crypto.Decryptor;
import org.interledger.ilpv4.connector.config.BalanceTrackerConfig;
import org.interledger.ilpv4.connector.config.RedisConfig;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import redis.embedded.RedisServerBuilder;

import java.util.Objects;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.CLEARING_BALANCE;
import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.PREPAID_AMOUNT;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and clearingBalance-change functionality for
 * handling Fulfill packets.
 */
public abstract class AbstractRedisBalanceTrackerTest {

  protected static final int REDIS_PORT = 6379;

  protected static final AccountId ACCOUNT_ID = AccountId.of("1");

  protected static final boolean PRODUCES_NO_ERROR = false;
  protected static final boolean PRODUCES_ERROR = true;
  protected static final Optional<Long> NO_MIN_BALANCE = Optional.empty();

  protected static final long ZERO = 0L;
  protected static final long ONE = 1L;
  protected static final long NEGATIVE_ONE = -1L;
  protected static final long TWO = 2L;
  protected static final long NEGATIVE_TWO = -2L;
  protected static final long NINE = 9L;
  protected static final long TEN = 10L;
  protected static final long NEGATIVE_TEN = -10L;
  protected static final long PREPARE_ONE = ONE;
  protected static final long PREPARE_TEN = TEN;
  protected static final long TWENTY = 20L;
  protected static final Optional<Long> ZERO_MIN = Optional.of(ZERO);
  protected static final Optional<Long> ONE_MIN = Optional.of(ONE);
  protected static final Optional<Long> NEG_ONE_MIN = Optional.of(NEGATIVE_ONE);
  protected static final Optional<Long> NEG_TEN_MIN = Optional.of(NEGATIVE_TEN);

  private static redis.embedded.RedisServer redisServer;

  protected long existingClearingBalance;
  protected long existingPrepaidBalance;
  protected long prepareAmount;
  protected long expectedClearingBalanceInRedis;
  protected long expectedPrepaidAmountInRedis;

  /**
   * Required-args Constructor.
   */
  public AbstractRedisBalanceTrackerTest(
    final long existingClearingBalance,
    final long existingPrepaidBalance,
    final long prepareAmount,
    final long expectedClearingBalanceInRedis,
    final long expectedPrepaidAmountInRedis
  ) {
    this.existingClearingBalance = existingClearingBalance;
    this.existingPrepaidBalance = existingPrepaidBalance;
    this.prepareAmount = prepareAmount;
    this.expectedClearingBalanceInRedis = expectedClearingBalanceInRedis;
    this.expectedPrepaidAmountInRedis = expectedPrepaidAmountInRedis;
  }

  @BeforeClass
  public static void startRedisServer() {
    redisServer = new RedisServerBuilder().port(REDIS_PORT)
      //.setting("maxheap 128M")
      .build();
    redisServer.start();
  }

  @AfterClass
  public static void stopRedisServer() {
    redisServer.stop();
  }

  protected abstract RedisTemplate getRedisTemplate();

  protected void initializeAccount(final AccountId accountId, final long clearingBalance, final long existingPrepaidBalance) {
    getRedisTemplate().boundHashOps(toRedisAccountId(accountId)).put(CLEARING_BALANCE, clearingBalance + "");
    getRedisTemplate().boundHashOps(toRedisAccountId(accountId)).put(PREPAID_AMOUNT, existingPrepaidBalance + "");
  }

  protected String toRedisAccountId(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return "accounts:" + accountId.value();
  }

  @Configuration
  @Import({RedisConfig.class, BalanceTrackerConfig.class})
  static class Config {

    // For testing purposes, Redis is not secured with a password, so this implementation can be a no-op.
    @Bean
    Decryptor decryptor() {
      return (keyMetadata, encryptionAlgorithm, cipherMessage) -> new byte[0];
    }


    @Bean
    ObjectMapper objectMapper() {
      return ObjectMapperFactory.create();
    }
  }
}
