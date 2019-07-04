package org.interledger.ilpv4.connector.balances;

import com.google.common.collect.ImmutableList;
import com.sappenin.interledger.ilpv4.connector.balances.AccountBalance;
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
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Reject packets.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {AbstractRedisBalanceTrackerTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerRejectPacketTest extends AbstractRedisBalanceTrackerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  RedisBalanceTracker balanceTracker;

  @Autowired
  RedisTemplate<String, String> redisTemplate;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerRejectPacketTest(
    final long existingAccountBalance,
    final long existingPrepaidBalance,
    final long prepareAmount,
    final long expectedBalanceInRedis,
    final long expectedPrepaidAmountInRedis
  ) {
    super(
      existingAccountBalance, existingPrepaidBalance, prepareAmount, expectedBalanceInRedis,
      expectedPrepaidAmountInRedis
    );
  }

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
      // existing_account_balance, existing_prepaid_amount,
      // prepare_amount,
      // expected_balance, expected_prepaid_amount

      // clearingBalance = 0, prepaid_amount = 0
      new Object[]{ZERO, ZERO, PREPARE_ONE, ONE, ZERO},
      // clearingBalance = 0, prepaid_amount > 0
      new Object[]{ZERO, ONE, PREPARE_ONE, ONE, ONE},
      // clearingBalance = 0, prepaid_amount < 0
      new Object[]{ZERO, NEGATIVE_ONE, PREPARE_ONE, ONE, NEGATIVE_ONE},

      // clearingBalance > 0, prepaid_amount = 0
      new Object[]{ONE, ZERO, PREPARE_ONE, TWO, ZERO},
      // clearingBalance > 0, prepaid_amount > 0
      new Object[]{ONE, ONE, PREPARE_ONE, TWO, ONE},
      // clearingBalance > 0, prepaid_amount < 0
      new Object[]{ONE, NEGATIVE_ONE, PREPARE_ONE, TWO, NEGATIVE_ONE},

      // clearingBalance < 0, prepaid_amount = 0
      new Object[]{NEGATIVE_ONE, ZERO, PREPARE_ONE, ZERO, ZERO},
      // clearingBalance < 0, prepaid_amount > 0
      new Object[]{NEGATIVE_ONE, ONE, PREPARE_ONE, ZERO, ONE},
      // clearingBalance < 0, prepaid_amount < 0
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, ZERO, NEGATIVE_ONE},

      // Prepaid amt > from_amt
      new Object[]{NEGATIVE_ONE, TEN, PREPARE_ONE, ZERO, TEN},
      // Prepaid_amt < from_amt, but > 0
      new Object[]{TEN, ONE, PREPARE_TEN, TWENTY, ONE}
    );
  }

  @Override
  protected RedisTemplate getRedisTemplate() {
    return this.redisTemplate;
  }

  /////////////////
  // Reject Script (Null Checks)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void updateBalanceForRejectWithNullAccountId() {
    try {
      balanceTracker.updateBalanceForReject(null, ONE);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAccountId must not be null"));
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
    balanceTracker.updateBalanceForReject(accountId, ONE);

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.clearingBalance(), is(ONE));
    assertThat(loadedBalance.prepaidAmount(), is(ZERO));
    assertThat(loadedBalance.netBalance().longValue(), is(ONE));
  }

  @Test
  public void updateBalanceForRejectWithParamterizedValues() {
    this.initializeAccount(SOURCE_ACCOUNT_ID, this.existingAccountBalance, this.existingPrepaidBalance);

    balanceTracker.updateBalanceForReject(
      SOURCE_ACCOUNT_ID, this.prepareAmount
    );

    final AccountBalance loadedBalance = balanceTracker.getBalance(SOURCE_ACCOUNT_ID);
    assertThat(loadedBalance.clearingBalance(), is(expectedBalanceInRedis));
    assertThat(loadedBalance.prepaidAmount(), is(expectedPrepaidAmountInRedis));
    assertThat(loadedBalance.netBalance().longValue(), is(expectedBalanceInRedis + expectedPrepaidAmountInRedis));
  }
}
