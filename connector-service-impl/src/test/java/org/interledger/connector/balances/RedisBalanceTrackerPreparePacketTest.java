package org.interledger.connector.balances;

import com.google.common.collect.ImmutableList;
import org.interledger.connector.accounts.AccountId;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Prepare packets.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {AbstractRedisBalanceTrackerTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerPreparePacketTest extends AbstractRedisBalanceTrackerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  RedisBalanceTracker balanceTracker;

  @Autowired
  RedisTemplate<String, String> redisTemplate;

  private Optional<Long> minBalance;
  private boolean producesError;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerPreparePacketTest(
    final long existingAccountBalance,
    final long existingPrepaidBalance,
    final long prepareAmount,
    final Optional<Long> minBalance,
    final long expectedBalanceInRedis,
    final long expectedPrepaidAmountInRedis,
    final boolean producesError
  ) {
    super(
      existingAccountBalance, existingPrepaidBalance, prepareAmount, expectedBalanceInRedis,
      expectedPrepaidAmountInRedis
    );
    this.minBalance = minBalance;
    this.producesError = producesError;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
      // existing_account_balance, existing_prepaid_amount,
      // prepare_amount, min_balance,
      // expected_balance, expected_prepaid_amount,
      // producesError

      // clearingBalance = 0, prepaid_amount = 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ZERO, ZERO, PREPARE_ONE, NO_MIN_BALANCE, NEGATIVE_ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ZERO, PREPARE_ONE, NEG_ONE_MIN, NEGATIVE_ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ZERO, PREPARE_ONE, ONE_MIN, ZERO, ZERO, PRODUCES_ERROR},
      new Object[]{ZERO, ZERO, PREPARE_ONE, ZERO_MIN, ZERO, ZERO, PRODUCES_ERROR},

      // clearingBalance = 0, prepaid_amount > 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ZERO, ONE, PREPARE_ONE, NO_MIN_BALANCE, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ONE, PREPARE_ONE, NEG_ONE_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ZERO, ONE, PREPARE_ONE, ONE_MIN, ZERO, ZERO, PRODUCES_ERROR},
      new Object[]{ZERO, ONE, PREPARE_ONE, ZERO_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},

      // clearingBalance = 0, prepaid_amount < 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ZERO, NEGATIVE_ONE, PREPARE_ONE, NO_MIN_BALANCE, NEGATIVE_ONE, NEGATIVE_ONE, PRODUCES_NO_ERROR},
      new Object[]{ZERO, NEGATIVE_ONE, PREPARE_ONE, NEG_ONE_MIN, ZERO, NEGATIVE_ONE, PRODUCES_ERROR},
      new Object[]{ZERO, NEGATIVE_ONE, PREPARE_ONE, ONE_MIN, ZERO, NEGATIVE_ONE, PRODUCES_ERROR},
      new Object[]{ZERO, NEGATIVE_ONE, PREPARE_ONE, ZERO_MIN, ZERO, NEGATIVE_ONE, PRODUCES_ERROR},

      // clearingBalance > 0, prepaid_amount = 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE, ZERO, PREPARE_ONE, NO_MIN_BALANCE, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ZERO, PREPARE_ONE, NEG_ONE_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ZERO, PREPARE_ONE, ONE_MIN, ONE, ZERO, PRODUCES_ERROR},
      new Object[]{ONE, ZERO, PREPARE_ONE, ZERO_MIN, ZERO, ZERO, PRODUCES_NO_ERROR},

      // clearingBalance > 0, prepaid_amount > 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE, ONE, PREPARE_ONE, NO_MIN_BALANCE, ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE, PREPARE_ONE, NEG_ONE_MIN, ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE, PREPARE_ONE, ONE_MIN, ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{ONE, ONE, PREPARE_ONE, ZERO_MIN, ONE, ZERO, PRODUCES_NO_ERROR},

      // clearingBalance > 0, prepaid_amount < 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{ONE, NEGATIVE_ONE, PREPARE_ONE, NO_MIN_BALANCE, ZERO, NEGATIVE_ONE, PRODUCES_NO_ERROR},
      new Object[]{ONE, NEGATIVE_ONE, PREPARE_ONE, NEG_ONE_MIN, ZERO, NEGATIVE_ONE, PRODUCES_NO_ERROR},
      new Object[]{ONE, NEGATIVE_ONE, PREPARE_ONE, ONE_MIN, ONE, NEGATIVE_ONE, PRODUCES_ERROR},
      new Object[]{ONE, NEGATIVE_ONE, PREPARE_ONE, ZERO_MIN, ONE, NEGATIVE_ONE, PRODUCES_ERROR},

      // clearingBalance < 0, prepaid_amount = 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{NEGATIVE_ONE, ZERO, PREPARE_ONE, NO_MIN_BALANCE, NEGATIVE_TWO, ZERO, PRODUCES_NO_ERROR},
      new Object[]{NEGATIVE_ONE, ZERO, PREPARE_ONE, NEG_ONE_MIN, NEGATIVE_ONE, ZERO, PRODUCES_ERROR},
      new Object[]{NEGATIVE_ONE, ZERO, PREPARE_ONE, ONE_MIN, NEGATIVE_ONE, ZERO, PRODUCES_ERROR},
      new Object[]{NEGATIVE_ONE, ZERO, PREPARE_ONE, ZERO_MIN, NEGATIVE_ONE, ZERO, PRODUCES_ERROR},

      // clearingBalance < 0, prepaid_amount > 0
      // --> no min.
      // --> negative min
      // --> positive min
      // --> min = 0
      new Object[]{NEGATIVE_ONE, ONE, PREPARE_ONE, NO_MIN_BALANCE, NEGATIVE_ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{NEGATIVE_ONE, ONE, PREPARE_ONE, NEG_ONE_MIN, NEGATIVE_ONE, ZERO, PRODUCES_NO_ERROR},
      new Object[]{NEGATIVE_ONE, ONE, PREPARE_ONE, ONE_MIN, NEGATIVE_ONE, ONE, PRODUCES_ERROR},
      new Object[]{NEGATIVE_ONE, ONE, PREPARE_ONE, ZERO_MIN, NEGATIVE_ONE, ONE, PRODUCES_ERROR},

      // clearingBalance < 0, prepaid_amount < 0
      // --> no min.
      // --> negative min
      // --> min below prepare
      // --> positive min
      // --> min = 0
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, NO_MIN_BALANCE, NEGATIVE_TWO, NEGATIVE_ONE,
        PRODUCES_NO_ERROR},
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, NEG_ONE_MIN, NEGATIVE_ONE, NEGATIVE_ONE, PRODUCES_ERROR},
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, NEG_TEN_MIN, NEGATIVE_TWO, NEGATIVE_ONE, PRODUCES_NO_ERROR},
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, ONE_MIN, NEGATIVE_ONE, NEGATIVE_ONE, PRODUCES_ERROR},
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, ZERO_MIN, NEGATIVE_ONE, NEGATIVE_ONE, PRODUCES_ERROR},

      // Prepaid amt > from_amt
      new Object[]{NEGATIVE_ONE, TEN, PREPARE_ONE, ZERO_MIN, NEGATIVE_ONE, 9L, PRODUCES_NO_ERROR},
      // Prepaid_amt < from_amt, but > 0
      new Object[]{TEN, ONE, TEN, ZERO_MIN, ONE, ZERO, PRODUCES_NO_ERROR}
    );
  }

  @Override
  protected RedisTemplate getRedisTemplate() {
    return this.redisTemplate;
  }

  /////////////////
  // Prepare Script (Null Checks)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void updateBalanceForPrepareWithNullAccountId() {
    try {
      balanceTracker.updateBalanceForPrepare(null, ONE, Optional.ofNullable(ZERO));
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAccountId must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void updateBalanceForPrepareWithNullMinBalance() {
    try {
      balanceTracker.updateBalanceForPrepare(ACCOUNT_ID, ONE, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("minBalance must not be null"));
      throw e;
    }
  }

  /////////////////
  // Prepare Script (No Account in Redis)
  /////////////////

  /**
   * Verify the correct clearingBalance when no account exists in Redis.
   */
  @Test
  public void updateBalanceForPrepareWhenNoAccountInRedis() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    balanceTracker.updateBalanceForPrepare(
      accountId, ONE, Optional.ofNullable(NEGATIVE_TEN)
    );

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.clearingBalance(), is(NEGATIVE_ONE));
    assertThat(loadedBalance.prepaidAmount(), is(ZERO));
    assertThat(loadedBalance.netBalance().longValue(), is(NEGATIVE_ONE));
  }

  /**
   * Verify the correct clearingBalance when no account exists in Redis, but the minimum value is 0.
   */
  @Test(expected = BalanceTrackerException.class)
  public void updateBalanceForPrepareWhenNoAccountInRedisZeroMinBalance() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    try {
      balanceTracker.updateBalanceForPrepare(accountId, ONE, Optional.ofNullable(ZERO));
      fail("should have failed but did not!");
    } catch (BalanceTrackerException e) {
      assertTrue(e.getMessage().contains(
        String.format("Error handling prepare with sourceAmount `%s` from accountId `%s`", ONE, accountId.value(), ZERO,
          ZERO))
      );
      throw e;
    }
  }

  /**
   * Verify the correct clearingBalance when no account exists in Redis and the minBalance is unspecified.
   */
  @Test
  public void updateBalanceForPrepareWhenNoAccountInRedisAndNoMinBalance() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    balanceTracker.updateBalanceForPrepare(accountId, ONE, Optional.empty());

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.clearingBalance(), is(NEGATIVE_ONE));
    assertThat(loadedBalance.prepaidAmount(), is(ZERO));
    assertThat(loadedBalance.netBalance().longValue(), is(NEGATIVE_ONE));
  }

  /**
   * Verify the correct clearingBalance when the `min_balance` is greater than the
   */
  @Test
  public void updateBalanceForPrepareWithParamterizedValues() {

    this.initializeAccount(ACCOUNT_ID, this.existingClearingBalance, this.existingPrepaidBalance);

    if (producesError) {
      try {
        if (this.minBalance.isPresent()) {
          balanceTracker.updateBalanceForPrepare(ACCOUNT_ID, prepareAmount, minBalance);
        } else {
          balanceTracker.updateBalanceForPrepare(ACCOUNT_ID, prepareAmount, Optional.empty());
        }
        fail(String.format("Should have produced an error, but did not. " +
            "prepareAmount: `%s`, minBalance: `%s`, " +
            "existingClearingBalance: `%s`, existingPrepaidBalance: `%s`, " +
            "expectedClearingBalanceInRedis: `%s`, expectedPrepaidAmountInRedis: `%s`",
          prepareAmount, minBalance,
          this.existingClearingBalance, this.existingPrepaidBalance,
          this.expectedClearingBalanceInRedis, this.expectedPrepaidAmountInRedis)
        );
      } catch (BalanceTrackerException e) {
        assertTrue(e.getMessage().contains(
          String.format("Error handling prepare with sourceAmount `%s` from accountId `%s`",
            prepareAmount, ACCOUNT_ID.value(), existingClearingBalance,
            minBalance.map(Object::toString).orElse(""))));
        return;
      }
    } else {

      if (this.minBalance.isPresent()) {
        balanceTracker.updateBalanceForPrepare(
          ACCOUNT_ID, this.prepareAmount, this.minBalance
        );
      } else {
        balanceTracker.updateBalanceForPrepare(
          ACCOUNT_ID, this.prepareAmount, Optional.empty()
        );
      }

      final AccountBalance loadedBalance = balanceTracker.getBalance(ACCOUNT_ID);
      assertThat(loadedBalance.clearingBalance(), is(expectedClearingBalanceInRedis));
      assertThat(loadedBalance.prepaidAmount(), is(expectedPrepaidAmountInRedis));
      assertThat(loadedBalance.netBalance().longValue(), is(
        expectedClearingBalanceInRedis + expectedPrepaidAmountInRedis));
    }
  }
}
