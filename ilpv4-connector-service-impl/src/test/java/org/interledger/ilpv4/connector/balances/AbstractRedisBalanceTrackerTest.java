package org.interledger.ilpv4.connector.balances;

import org.interledger.connector.accounts.AccountId;
import org.interledger.crypto.Decryptor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import redis.embedded.RedisServerBuilder;

import java.util.Objects;
import java.util.Optional;

import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.BALANCE;
import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.PREPAID_AMOUNT;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Fulfill packets.
 */
public abstract class AbstractRedisBalanceTrackerTest {

  protected static final int REDIS_PORT = 6379;

  protected static final AccountId SOURCE_ACCOUNT_ID = AccountId.of("1");
  protected static final AccountId DESTINATION_ACCOUNT_ID = AccountId.of("1");

  protected static final boolean PRODUCES_NO_ERROR = false;
  protected static final boolean PRODUCES_ERROR = true;
  protected static final Optional<Long> NO_MIN_BALANCE = Optional.empty();

  protected static final long ZERO = 0L;
  protected static final long ONE = 1L;
  protected static final long NEGATIVE_ONE = -1L;
  protected static final long TWO = 2L;
  protected static final long NEGATIVE_TWO = -2L;
  protected static final long TEN = 10L;
  protected static final long NEGATIVE_TEN = -10L;
  protected static final long PREPARE_ONE = ONE;
  protected static final long PREPARE_TEN = TEN;
  protected static final long TWENTY = 20L;
  protected static final Optional<Long> ZERO_MIN = Optional.of(ZERO);
  protected static final Optional<Long> ONE_MIN = Optional.of(ONE);
  protected static final Optional<Long> NEG_ONE_MIN = Optional.of(NEGATIVE_ONE);
  protected static final Optional<Long> NEG_TEN_MIN = Optional.of(NEGATIVE_TEN);

  static redis.embedded.RedisServer redisServer;

  long existingAccountBalance;
  long existingPrepaidBalance;
  long prepareAmount;
  long expectedBalanceInRedis;
  long expectedPrepaidAmountInRedis;

  /**
   * Required-args Constructor.
   */
  public AbstractRedisBalanceTrackerTest(
    final long existingAccountBalance,
    final long existingPrepaidBalance,
    final long prepareAmount,
    final long expectedBalanceInRedis,
    final long expectedPrepaidAmountInRedis
  ) {
    this.existingAccountBalance = existingAccountBalance;
    this.existingPrepaidBalance = existingPrepaidBalance;
    this.prepareAmount = prepareAmount;
    this.expectedBalanceInRedis = expectedBalanceInRedis;
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

  protected void initializeAccount(final AccountId accountId, final long balance, final long existingPrepaidBalance) {
    getRedisTemplate().boundHashOps(toRedisAccountId(accountId)).put(BALANCE, balance + "");
    getRedisTemplate().boundHashOps(toRedisAccountId(accountId)).put(PREPAID_AMOUNT, existingPrepaidBalance + "");
  }

  protected String toRedisAccountId(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return "accounts:" + accountId.value();
  }

  @Configuration
  static class Config {

    // For testing purposes, Redis is not secured with a password, so this implementation can be a no-op.
    @Bean
    Decryptor decryptor() {
      return (keyMetadata, encryptionAlgorithm, cipherMessage) -> new byte[0];
    }
  }
}