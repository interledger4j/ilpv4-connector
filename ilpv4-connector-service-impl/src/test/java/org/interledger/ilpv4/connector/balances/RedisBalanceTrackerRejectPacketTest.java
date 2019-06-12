package org.interledger.ilpv4.connector.balances;

import com.google.common.collect.ImmutableList;
import com.sappenin.interledger.ilpv4.connector.balances.AccountBalance;
import org.interledger.connector.accounts.AccountId;
import org.interledger.crypto.Decryptor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import redis.embedded.RedisServerBuilder;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.BALANCE;
import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.PREPAID_AMOUNT;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Reject packets.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {RedisBalanceTrackerConfig.class, RedisBalanceTrackerRejectPacketTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerRejectPacketTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  private static final int REDIS_PORT = 6379;
  private static final AccountId SOURCE_ACCOUNT_ID = AccountId.of("1");

  private static final BigInteger TWO = BigInteger.valueOf(2L);
  private static final BigInteger PREPARE_ONE = ONE;
  private static final BigInteger PREPARE_TEN = TEN;

  private static redis.embedded.RedisServer redisServer;

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  RedisBalanceTracker balanceTracker;

  @Autowired
  RedisTemplate<String, String> redisTemplate;

  private BigInteger existingAccountBalance;
  private BigInteger existingPrepaidBalance;
  private BigInteger prepareAmount;
  private BigInteger expectedBalanceInRedis;
  private BigInteger expectedPrepaidAmountInRedis;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerRejectPacketTest(
    final BigInteger existingAccountBalance,
    final BigInteger existingPrepaidBalance,
    final BigInteger prepareAmount,
    final BigInteger expectedBalanceInRedis,
    final BigInteger expectedPrepaidAmountInRedis
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

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
      // existing_account_balance, existing_prepaid_amount,
      // prepare_amount,
      // expected_balance, expected_prepaid_amount

      // balance = 0, prepaid_amount = 0
      new Object[]{ZERO, ZERO, PREPARE_ONE, ONE, ZERO},
      // balance = 0, prepaid_amount > 0
      new Object[]{ZERO, ONE, PREPARE_ONE, ONE, ONE},
      // balance = 0, prepaid_amount < 0
      new Object[]{ZERO, ONE.negate(), PREPARE_ONE, ONE, ONE.negate()},

      // balance > 0, prepaid_amount = 0
      new Object[]{ONE, ZERO, PREPARE_ONE, TWO, ZERO},
      // balance > 0, prepaid_amount > 0
      new Object[]{ONE, ONE, PREPARE_ONE, TWO, ONE},
      // balance > 0, prepaid_amount < 0
      new Object[]{ONE, ONE.negate(), PREPARE_ONE, TWO, ONE.negate()},

      // balance < 0, prepaid_amount = 0
      new Object[]{ONE.negate(), ZERO, PREPARE_ONE, ZERO, ZERO},
      // balance < 0, prepaid_amount > 0
      new Object[]{ONE.negate(), ONE, PREPARE_ONE, ZERO, ONE},
      // balance < 0, prepaid_amount < 0
      new Object[]{ONE.negate(), ONE.negate(), PREPARE_ONE, ZERO, ONE.negate()},

      // Prepaid amt > from_amt
      new Object[]{ONE.negate(), TEN, PREPARE_ONE, ZERO, TEN},
      // Prepaid_amt < from_amt, but > 0
      new Object[]{TEN, ONE, PREPARE_TEN, BigInteger.valueOf(20L), ONE}
    );
  }


  /////////////////
  // Reject Script (Null Checks)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void updateBalanceForRejectWithNullAccountId() {
    try {
      balanceTracker.updateBalanceForReject(null, ONE);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAccountId must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void updateBalanceForRejectWithNullFromAmount() {
    try {
      balanceTracker.updateBalanceForReject(SOURCE_ACCOUNT_ID, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAmount must not be null!"));
      throw e;
    }
  }

  /////////////////
  // Reject Script (No Account in Redis)
  /////////////////

  /**
   * Verify the correct operation when no account exists in Redis.
   */
  @Test
  public void updateBalanceForRejectWhenNoAccountInRedis() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final BigInteger balance = balanceTracker.updateBalanceForReject(accountId, ONE);

    assertThat(balance, is(BigInteger.valueOf(1L)));

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.balance(), is(BigInteger.valueOf(1L)));
    assertThat(loadedBalance.prepaidBalance(), is(ZERO));
    assertThat(loadedBalance.netBalance(), is(ONE));
  }

  @Test
  public void updateBalanceForRejectWithParamterizedValues() {
    this.initializeAccount(SOURCE_ACCOUNT_ID, this.existingAccountBalance, this.existingPrepaidBalance);

    final BigInteger net_balance = balanceTracker.updateBalanceForReject(
      SOURCE_ACCOUNT_ID, this.prepareAmount
    );

    assertThat(net_balance, is(expectedBalanceInRedis.add(expectedPrepaidAmountInRedis)));

    final AccountBalance loadedBalance = balanceTracker.getBalance(SOURCE_ACCOUNT_ID);
    assertThat(loadedBalance.balance(), is(expectedBalanceInRedis));
    assertThat(loadedBalance.prepaidBalance(), is(expectedPrepaidAmountInRedis));
    assertThat(loadedBalance.netBalance(), is(expectedBalanceInRedis.add(expectedPrepaidAmountInRedis)));
  }

  protected void initializeAccount(final AccountId accountId, final BigInteger balance, final BigInteger minBalance) {
    redisTemplate.boundHashOps(toRedisAccountId(accountId)).put(BALANCE, balance.toString());
    redisTemplate.boundHashOps(toRedisAccountId(accountId)).put(PREPAID_AMOUNT, minBalance.toString());
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