package org.interledger.ilpv4.connector.balances;

import com.google.common.collect.ImmutableList;
import com.sappenin.interledger.ilpv4.connector.balances.AccountBalance;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
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
import java.util.Optional;
import java.util.UUID;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.BALANCE;
import static org.interledger.ilpv4.connector.balances.RedisBalanceTracker.PREPAID_AMOUNT;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Prepare packets.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {RedisBalanceTrackerConfig.class, RedisBalanceTrackerPreparePacketTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerPreparePacketTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  private static final int REDIS_PORT = 6379;
  private static final AccountId SOURCE_ACCOUNT_ID = AccountId.of("1");
  private static final boolean PRODUCES_NO_ERROR = false;
  private static final boolean PRODUCES_ERROR = true;
  private static final BigInteger NO_MIN_BALANCE = null;

  private static final BigInteger TWO = BigInteger.valueOf(2L);
  private static final BigInteger PREPARE_ONE = ONE;
  private static final BigInteger ZERO_MIN = ZERO;
  private static final BigInteger ONE_MIN = ONE;
  private static final BigInteger NEG_ONE_MIN = ONE.negate();
  private static final BigInteger NEG_TEN_MIN = TEN.negate();

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
  private Optional<BigInteger> minBalance;
  private BigInteger expectedBalanceInRedis;
  private BigInteger expectedPrepaidAmountInRedis;
  private boolean producesError;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerPreparePacketTest(
    final BigInteger existingAccountBalance,
    final BigInteger existingPrepaidBalance,
    final BigInteger prepareAmount,
    final BigInteger minBalance,
    final BigInteger expectedBalanceInRedis,
    final BigInteger expectedPrepaidAmountInRedis,
    final boolean producesError
  ) {
    this.existingAccountBalance = existingAccountBalance;
    this.existingPrepaidBalance = existingPrepaidBalance;
    this.prepareAmount = prepareAmount;
    this.minBalance = Optional.ofNullable(minBalance);
    this.expectedBalanceInRedis = expectedBalanceInRedis;
    this.expectedPrepaidAmountInRedis = expectedPrepaidAmountInRedis;
    this.producesError = producesError;
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
      // prepare_amount, min_balance,
      // expected_balance, expected_prepaid_amount,
      // producesError

      // balance = 0, prepaid_amount = 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ZERO, ZERO, PREPARE_ONE, NO_MIN_BALANCE, BigInteger.valueOf(-1), ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ZERO, PREPARE_ONE, NEG_ONE_MIN, BigInteger.valueOf(-1), ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ZERO, PREPARE_ONE, ONE_MIN, ZERO, ZERO, PRODUCES_ERROR},
      new Object[]{ZERO, ZERO, PREPARE_ONE, ZERO_MIN, ZERO, ZERO, PRODUCES_ERROR},

      // balance = 0, prepaid_amount > 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ZERO, ONE, PREPARE_ONE, NO_MIN_BALANCE, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ONE, PREPARE_ONE, NEG_ONE_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ONE, PREPARE_ONE, ONE_MIN, ZERO, ZERO, PRODUCES_ERROR},
      new Object[]{ZERO, ONE, PREPARE_ONE, ZERO_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},

      // balance = 0, prepaid_amount < 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ZERO, ONE.negate(), PREPARE_ONE, NO_MIN_BALANCE, ONE.negate(), ONE.negate(), PRODUCES_NO_ERROR},
      new Object[]{ZERO, ONE.negate(), PREPARE_ONE, NEG_ONE_MIN, ZERO, ONE.negate(), PRODUCES_ERROR},
      new Object[]{ZERO, ONE.negate(), PREPARE_ONE, ONE_MIN, ZERO, ONE.negate(), PRODUCES_ERROR},
      new Object[]{ZERO, ONE.negate(), PREPARE_ONE, ZERO_MIN, ZERO, ONE.negate(), PRODUCES_ERROR},

      // balance > 0, prepaid_amount = 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE, ZERO, PREPARE_ONE, NO_MIN_BALANCE, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ZERO, PREPARE_ONE, NEG_ONE_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ZERO, PREPARE_ONE, ONE_MIN, ONE, ZERO, PRODUCES_ERROR},
      new Object[]{ONE, ZERO, PREPARE_ONE, ZERO_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},

      // balance > 0, prepaid_amount > 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE, ONE, PREPARE_ONE, NO_MIN_BALANCE, ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE, PREPARE_ONE, NEG_ONE_MIN, ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE, PREPARE_ONE, ONE_MIN, ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE, PREPARE_ONE, ZERO_MIN, ONE, ZERO, PRODUCES_NO_ERROR},

      // balance > 0, prepaid_amount < 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE, ONE.negate(), PREPARE_ONE, NO_MIN_BALANCE, ZERO, ONE.negate(), PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE.negate(), PREPARE_ONE, NEG_ONE_MIN, ZERO, ONE.negate(), PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE.negate(), PREPARE_ONE, ONE_MIN, ONE, ONE.negate(), PRODUCES_ERROR},
      new Object[]{ONE, ONE.negate(), PREPARE_ONE, ZERO_MIN, ONE, ONE.negate(), PRODUCES_ERROR},

      // balance < 0, prepaid_amount = 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE.negate(), ZERO, PREPARE_ONE, NO_MIN_BALANCE, TWO.negate(), ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE.negate(), ZERO, PREPARE_ONE, NEG_ONE_MIN, ONE.negate(), ZERO, PRODUCES_ERROR},
      new Object[]{ONE.negate(), ZERO, PREPARE_ONE, ONE_MIN, ONE.negate(), ZERO, PRODUCES_ERROR},
      new Object[]{ONE.negate(), ZERO, PREPARE_ONE, ZERO_MIN, ONE.negate(), ZERO, PRODUCES_ERROR},

      // balance < 0, prepaid_amount > 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE.negate(), ONE, PREPARE_ONE, NO_MIN_BALANCE, ONE.negate(), ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE.negate(), ONE, PREPARE_ONE, NEG_ONE_MIN, ONE.negate(), ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE.negate(), ONE, PREPARE_ONE, ONE_MIN, ONE.negate(), ONE, PRODUCES_ERROR},
      new Object[]{ONE.negate(), ONE, PREPARE_ONE, ZERO_MIN, ONE.negate(), ONE, PRODUCES_ERROR},

      // balance < 0, prepaid_amount < 0
      // --> no min.
      // --> negative min
      // --> min below prepare
      // --> positive min
      // --> min = 0
      new Object[]{ONE.negate(), ONE.negate(), PREPARE_ONE, NO_MIN_BALANCE, TWO.negate(), ONE.negate(),
        PRODUCES_NO_ERROR},
      new Object[]{ONE.negate(), ONE.negate(), PREPARE_ONE, NEG_ONE_MIN, ONE.negate(), ONE.negate(), PRODUCES_ERROR},
      new Object[]{ONE.negate(), ONE.negate(), PREPARE_ONE, NEG_TEN_MIN, TWO.negate(), ONE.negate(), PRODUCES_NO_ERROR},
      new Object[]{ONE.negate(), ONE.negate(), PREPARE_ONE, ONE_MIN, ONE.negate(), ONE.negate(), PRODUCES_ERROR},
      new Object[]{ONE.negate(), ONE.negate(), PREPARE_ONE, ZERO_MIN, ONE.negate(), ONE.negate(), PRODUCES_ERROR},

      // Prepaid amt > from_amt
      new Object[]{ONE.negate(), TEN, PREPARE_ONE, ZERO_MIN, ONE.negate(), new BigInteger("9"), PRODUCES_NO_ERROR},
      // Prepaid_amt < from_amt, but > 0
      new Object[]{TEN, ONE, TEN, ZERO_MIN, ONE, ZERO, PRODUCES_NO_ERROR}
    );
  }


  /////////////////
  // Prepare Script (Null Checks)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void updateBalanceForPrepareWithNullAccountId() {
    try {
      balanceTracker.updateBalanceForPrepare(null, ONE, Optional.ofNullable(ZERO));
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAccountId must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void updateBalanceForPrepareWithNullFromAmount() {
    try {
      balanceTracker.updateBalanceForPrepare(SOURCE_ACCOUNT_ID, null, Optional.ofNullable(ZERO));
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAmount must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void updateBalanceForPrepareWithNullMinBalance() {
    try {
      balanceTracker.updateBalanceForPrepare(SOURCE_ACCOUNT_ID, ONE, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("minBalance must not be null!"));
      throw e;
    }
  }

  /////////////////
  // Prepare Script (No Account in Redis)
  /////////////////

  /**
   * Verify the correct balance when no account exists in Redis.
   */
  @Test
  public void updateBalanceForPrepareWhenNoAccountInRedis() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final BigInteger balance = balanceTracker.updateBalanceForPrepare(
      accountId, ONE, Optional.ofNullable(BigInteger.TEN.negate())
    );

    assertThat(balance, is(BigInteger.valueOf(-1L)));

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.balance(), is(BigInteger.valueOf(-1L)));
    assertThat(loadedBalance.prepaidBalance(), is(ZERO));
    assertThat(loadedBalance.netBalance(), is(ONE.negate()));
  }

  /**
   * Verify the correct balance when no account exists in Redis, but the minimum value is 0.
   */
  @Test(expected = BalanceTrackerException.class)
  public void updateBalanceForPrepareWhenNoAccountInRedisZeroMinBalance() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    try {
      balanceTracker.updateBalanceForPrepare(accountId, ONE, Optional.ofNullable(ZERO));
      fail("should have failed but did not!");
    } catch (BalanceTrackerException e) {
      assertTrue(e.getMessage().contains(
        String.format("Incoming prepare of %s would bring account accounts:%s under its minimum balance. " +
            "Current balance: %s, min balance: %s ",
          ONE.toString(), accountId.value(), ZERO.toString(), ZERO.toString()))
      );
      throw e;
    }
  }

  /**
   * Verify the correct balance when no account exists in Redis and the minBalance is unspecified.
   */
  @Test
  public void updateBalanceForPrepareWhenNoAccountInRedisAndNoMinBalance() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    final BigInteger balance = balanceTracker.updateBalanceForPrepare(accountId, ONE);

    assertThat(balance, is(BigInteger.valueOf(-1L)));

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.balance(), is(BigInteger.valueOf(-1L)));
    assertThat(loadedBalance.prepaidBalance(), is(ZERO));
    assertThat(loadedBalance.netBalance(), is(ONE.negate()));
  }

  /**
   * Verify the correct balance when the `min_balance` is greater than the
   */
  @Test
  public void updateBalanceForPrepareWithParamterizedValues() {

    this.initializeAccount(SOURCE_ACCOUNT_ID, this.existingAccountBalance, this.existingPrepaidBalance);

    if (producesError) {
      try {
        if (this.minBalance.isPresent()) {
          balanceTracker.updateBalanceForPrepare(SOURCE_ACCOUNT_ID, prepareAmount, minBalance);
        } else {
          balanceTracker.updateBalanceForPrepare(SOURCE_ACCOUNT_ID, prepareAmount);
        }
        fail(String.format("Should have produced an error, but did not. " +
            "prepareAmount: `%s`, minBalance: `%s`, " +
            "existingAccountBalance: `%s`, existingPrepaidBalance: `%s`, " +
            "expectedBalanceInRedis: `%s`, expectedPrepaidAmountInRedis: `%s`",
          prepareAmount, minBalance,
          this.existingAccountBalance, this.existingPrepaidBalance,
          this.expectedBalanceInRedis, this.expectedPrepaidAmountInRedis)
        );
      } catch (BalanceTrackerException e) {
        assertTrue(e.getMessage().contains(
          String.format("Incoming prepare of %s would bring account accounts:%s under its minimum balance. " +
              "Current balance: %s, min balance: %s ",
            prepareAmount.toString(), SOURCE_ACCOUNT_ID.value(), existingAccountBalance.toString(),
            minBalance.map(BigInteger::toString).orElse(""))));
        return;
      }
    } else {

      if (this.minBalance.isPresent()) {
        final BigInteger net_balance = balanceTracker.updateBalanceForPrepare(
          SOURCE_ACCOUNT_ID, this.prepareAmount, this.minBalance
        );

        assertThat(net_balance, is(expectedBalanceInRedis.add(expectedPrepaidAmountInRedis)));

      } else {
        final BigInteger net_balance = balanceTracker.updateBalanceForPrepare(
          SOURCE_ACCOUNT_ID, this.prepareAmount
        );

        assertThat(net_balance, is(expectedBalanceInRedis.add(expectedPrepaidAmountInRedis)));
      }

      final AccountBalance loadedBalance = balanceTracker.getBalance(SOURCE_ACCOUNT_ID);
      assertThat(loadedBalance.balance(), is(expectedBalanceInRedis));
      assertThat(loadedBalance.prepaidBalance(), is(expectedPrepaidAmountInRedis));
      assertThat(loadedBalance.netBalance(), is(expectedBalanceInRedis.add(expectedPrepaidAmountInRedis)));
    }
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