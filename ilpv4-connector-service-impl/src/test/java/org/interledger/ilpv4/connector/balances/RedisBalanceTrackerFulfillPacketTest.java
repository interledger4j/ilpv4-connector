package org.interledger.ilpv4.connector.balances;

import com.google.common.collect.ImmutableList;
import com.sappenin.interledger.ilpv4.connector.balances.AccountBalance;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Fulfill packets.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {AbstractRedisBalanceTrackerTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerFulfillPacketTest extends AbstractRedisBalanceTrackerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  RedisBalanceTracker balanceTracker;

  @Autowired
  RedisTemplate<String, String> redisTemplate;

  @Mock
  AccountSettings accountSettingsMock;
  @Mock
  AccountBalance accountBalanceMock;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerFulfillPacketTest(
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
      // existing_clearing_balance, existing_prepaid_amount,
      // settle_threshold, settle_to,
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
      new Object[]{TEN, ONE, PREPARE_TEN, 20L, ONE}
    );
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    AccountBalanceSettings balanceSettingsMock = mock(AccountBalanceSettings.class);
    when(accountSettingsMock.getBalanceSettings()).thenReturn(balanceSettingsMock);
    when(accountSettingsMock.getAccountId()).thenReturn(ACCOUNT_ID);
    when(accountSettingsMock.getAssetScale()).thenReturn(2); // Hard-coded since this is not being tested in this class.

    when(accountBalanceMock.clearingBalance()).thenReturn(existingClearingBalance);
    when(accountBalanceMock.prepaidAmount()).thenReturn(existingPrepaidBalance);
  }

  @Override
  protected RedisTemplate getRedisTemplate() {
    return this.redisTemplate;
  }

  /////////////////
  // Fulfill Script (Null Checks)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void updateBalanceForFulfillWithNullAccountId() {
    try {
      balanceTracker.updateBalanceForFulfill(null, ONE);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationAccountSettings must not be null"));
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
  public void updateBalanceForFulfillWhenNoAccountInRedis() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    when(accountSettingsMock.getAccountId()).thenReturn(accountId);
    balanceTracker.updateBalanceForFulfill(accountSettingsMock, ONE);

    final AccountBalance loadedBalance = balanceTracker.getBalance(accountId);
    assertThat(loadedBalance.clearingBalance(), is(ONE));
    assertThat(loadedBalance.prepaidAmount(), is(ZERO));
    assertThat(loadedBalance.netBalance().longValue(), is(ONE));
  }

  @Test
  public void updateBalanceForFulfillWithParamterizedValues() {
    this.initializeAccount(ACCOUNT_ID, this.existingClearingBalance, this.existingPrepaidBalance);

    balanceTracker.updateBalanceForFulfill(accountSettingsMock, this.prepareAmount);

    final AccountBalance loadedBalance = balanceTracker.getBalance(ACCOUNT_ID);
    assertThat(loadedBalance.clearingBalance(), is(expectedClearingBalanceInRedis));
    assertThat(loadedBalance.prepaidAmount(), is(expectedPrepaidAmountInRedis));
    assertThat(loadedBalance.netBalance().longValue(), is(expectedClearingBalanceInRedis + expectedPrepaidAmountInRedis));
  }

}
