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
 * incoming settlements.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {AbstractRedisBalanceTrackerTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerIncomingSettlementTest extends AbstractRedisBalanceTrackerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private RedisBalanceTracker balanceTracker;

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerIncomingSettlementTest(
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
      // settle_amount,
      // expected_clearing_balance, expected_prepaid_amount

      // clearingBalance = 0, prepaid_amount = 0
      new Object[]{ZERO, ZERO, PREPARE_ONE, ZERO, ONE}, //0
      // clearingBalance = 0, prepaid_amount > 0
      new Object[]{ZERO, ONE, PREPARE_ONE, ZERO, TWO}, //1
      // clearingBalance = 0, prepaid_amount < 0
      new Object[]{ZERO, NEGATIVE_ONE, PREPARE_ONE, ZERO, ZERO}, //2

      // clearingBalance > 0, prepaid_amount = 0
      new Object[]{ONE, ZERO, PREPARE_ONE, ONE, ONE}, //3
      // clearingBalance > 0, prepaid_amount > 0
      new Object[]{ONE, ONE, PREPARE_ONE, ONE, TWO}, //4
      // clearingBalance > 0, prepaid_amount < 0
      new Object[]{ONE, NEGATIVE_ONE, PREPARE_ONE, ONE, ZERO}, //5

      // clearingBalance < 0, prepaid_amount = 0
      new Object[]{NEGATIVE_ONE, ZERO, PREPARE_ONE, ZERO, ZERO}, //6
      // clearingBalance < 0, prepaid_amount > 0
      new Object[]{NEGATIVE_ONE, ONE, PREPARE_ONE, ZERO, ONE}, //7
      // clearingBalance < 0, prepaid_amount < 0
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, ZERO, NEGATIVE_ONE}, //8

      // Prepaid amt > from_amt
      new Object[]{NEGATIVE_ONE, TEN, PREPARE_ONE, ZERO, TEN}, //9
      // Prepaid_amt < from_amt, but > 0
      new Object[]{TEN, ONE, PREPARE_TEN, 10L, 11L} //10
    );
  }

  @Override
  protected RedisTemplate getRedisTemplate() {
    return this.redisTemplate;
  }

  /////////////////
  // Fulfill Script (Null Checks)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void updateBalanceForIncomingSettlementWithNullIdempotenceId() {
    try {
      balanceTracker.updateBalanceForIncomingSettlement(null, ACCOUNT_ID, ONE);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("idempotencyKey must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void updateBalanceForIncomingSettlementWithNullAccountId() {
    try {
      balanceTracker.updateBalanceForIncomingSettlement(UUID.randomUUID().toString(), null, ONE);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("accountId must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void updateBalanceForIncomingSettlementWithNegativeAmount() {
    try {
      balanceTracker.updateBalanceForIncomingSettlement(UUID.randomUUID().toString(), null, -10L);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("accountId must not be null"));
      throw e;
    }
  }

  /////////////////
  // Fulfill Script (No Account in Redis)
  /////////////////

  /**
   * Verify the correct operation when no account exists in Redis.
   */
  @Test
  public void updateBalanceForIncomingSettlementWhenNoAccountInRedis() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    balanceTracker.updateBalanceForIncomingSettlement(UUID.randomUUID().toString(), accountId, ONE);

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.clearingBalance(), is(ZERO));
    assertThat(loadedBalance.prepaidAmount(), is(ONE));
    assertThat(loadedBalance.netBalance().longValue(), is(ONE));
  }

  @Test
  public void updateBalanceForIncomingSettlementWithParamterizedValues() {
    this.initializeAccount(ACCOUNT_ID, this.existingClearingBalance, this.existingPrepaidBalance);

    balanceTracker.updateBalanceForIncomingSettlement(UUID.randomUUID().toString(), ACCOUNT_ID, this.prepareAmount);

    final AccountBalance loadedBalance = balanceTracker.getBalance(ACCOUNT_ID);
    assertThat(loadedBalance.clearingBalance(), is(expectedClearingBalanceInRedis));
    assertThat(loadedBalance.prepaidAmount(), is(expectedPrepaidAmountInRedis));
    assertThat(loadedBalance.netBalance().longValue(),
      is(expectedClearingBalanceInRedis + expectedPrepaidAmountInRedis));
  }

}
