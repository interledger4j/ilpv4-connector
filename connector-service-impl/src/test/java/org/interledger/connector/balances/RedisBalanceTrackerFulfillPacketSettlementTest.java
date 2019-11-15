package org.interledger.connector.balances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;

import com.google.common.collect.ImmutableList;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Fulfill packets as it relates to settlement.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {AbstractRedisBalanceTrackerTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerFulfillPacketSettlementTest extends AbstractRedisBalanceTrackerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  private static final long UNUSED = 0l;

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private RedisBalanceTracker balanceTracker;

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private AccountSettings accountSettingsMock;
  @Mock
  private AccountBalance accountBalanceMock;

  private Optional<Long> settleThreshold;
  private long settleTo;
  private long expectedClearingAmountToSettle;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerFulfillPacketSettlementTest(
    final long existingClearingBalance,
    final long existingPrepaidBalance,
    final long prepareAmount,
    final Optional<Long> settleThreshold,
    final long settleTo,
    final long expectedClearingAmountToSettle
  ) {
    super(
      existingClearingBalance, existingPrepaidBalance, prepareAmount, UNUSED, UNUSED
    );

    this.settleThreshold = settleThreshold;
    this.settleTo = settleTo;
    this.expectedClearingAmountToSettle = expectedClearingAmountToSettle;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
      // existing_clearing_balance, existing_prepaid_amount,
      // prepare_amount
      // settle_threshold, settle_to,
      // expectedClearingAmountToSettle

      // No Threshold...
      new Object[]{ZERO, ZERO, PREPARE_ONE, Optional.empty(), ZERO, 0L}, //0
      new Object[]{TEN, ZERO, PREPARE_ONE, Optional.empty(), ZERO, 0L}, //1
      new Object[]{TEN, TEN, PREPARE_ONE, Optional.empty(), ZERO, 0L}, //2
      new Object[]{TEN, TEN, PREPARE_ONE, Optional.empty(), ONE, 0L}, //3

      // Threshold = 10, SettleTo=0
      new Object[]{NINE, ZERO, PREPARE_ONE, Optional.of(10L), ZERO, 10L}, //4
      new Object[]{TEN, ZERO, PREPARE_ONE, Optional.of(10L), ZERO, 11L}, //5
      new Object[]{11L, ZERO, PREPARE_ONE, Optional.of(10L), ZERO, 12L}, //6
      new Object[]{11L, ONE, PREPARE_ONE, Optional.of(10L), ONE, 11L}, //7
      // Threshold = 5, SettleTo=5
      new Object[]{TEN, ZERO, PREPARE_ONE, Optional.of(5L), 5L, 6L}, //8
      new Object[]{NINE, ZERO, PREPARE_ONE, Optional.of(5L), 5L, 5L}, //9
      new Object[]{11L, ZERO, PREPARE_ONE, Optional.of(5L), 5L, 7L}, //10

      // Threshold = 5, SettleTo=10
      new Object[]{5L, 10L, PREPARE_ONE, Optional.of(10L), 5L, 0L}, //11
      new Object[]{5L, 10L, PREPARE_ONE, Optional.of(5L), 0L, 6L}, //12

      // clearing_balance (2) < settlement_threshold (3)
      new Object[]{1L, ZERO, PREPARE_ONE, Optional.of(3L), 0L, 0L}, //13
      new Object[]{1L, 2L, PREPARE_ONE, Optional.of(3L), 0L, 0L}, //14

      // clearing_balance (3) = settlement_threshold (3)
      new Object[]{2L, ZERO, PREPARE_ONE, Optional.of(3L), 0L, 3L}, //15
      new Object[]{2L, 2L, PREPARE_ONE, Optional.of(3L), 0L, 3L}, //16

      // clearing_balance (4) > settlement_threshold (3)
      new Object[]{3L, ZERO, PREPARE_ONE, Optional.of(3L), 0L, 4L}, //17
      new Object[]{3L, 2L, PREPARE_ONE, Optional.of(3L), 0L, 4L}, //18

      // clearing_balance (1) < settlement_threshold (2) (settle_to = 1)
      new Object[]{0L, ZERO, PREPARE_ONE, Optional.of(2L), 1L, 0L}, //19
      new Object[]{0L, 2L, PREPARE_ONE, Optional.of(2L), 1L, 0L}, //20

      // clearing_balance (2) = settlement_threshold (2)
      new Object[]{1L, ZERO, PREPARE_ONE, Optional.of(2L), 1L, 1L}, //19
      new Object[]{1L, 2L, PREPARE_ONE, Optional.of(2L), 1L, 1L}, //20

      // clearing_balance(3) > settlement_threshold(1)
      new Object[]{2L, ZERO, PREPARE_ONE, Optional.of(1L), 1L, 2L}, //21
      new Object[]{2L, 2L, PREPARE_ONE, Optional.of(1L), 1L, 2L}, //22

      /////////
      // clearing_balance > settle_threshold AND ...
      /////////

      // clearing_balance = settle_to (script will add 1 to 9 and then evaluate)
      new Object[]{9L, ZERO, PREPARE_ONE, Optional.of(5L), 10L, 0L}, //23
      new Object[]{9L, 10L, PREPARE_ONE, Optional.of(5L), 10L, 0L}, //24

      // clearing_balance < settle_to (script will add 1 to 8 and then evaluate)
      new Object[]{8L, ZERO, PREPARE_ONE, Optional.of(5L), 20L, 0L}, //25
      new Object[]{8L, 10L, PREPARE_ONE, Optional.of(5L), 20L, 0L}, //26

      // clearing_balance > settle_to (script will add 1 to 10 and then evaluate)
      new Object[]{10, ZERO, PREPARE_ONE, Optional.of(5L), 0L, 11L}, //27
      new Object[]{10, 10, PREPARE_ONE, Optional.of(5L), 0L, 11L}, //28

      // prepare amount zero (no-op) packets
      new Object[]{5L, 10L, PREPARE_ZERO, Optional.of(10L), 5L, 0L} //29
    );
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    AccountBalanceSettings balanceSettingsMock = mock(AccountBalanceSettings.class);
    when(balanceSettingsMock.settleTo()).thenReturn(settleTo);
    when(balanceSettingsMock.settleThreshold()).thenReturn(settleThreshold);
    when(accountSettingsMock.accountId()).thenReturn(ACCOUNT_ID);
    when(accountSettingsMock.balanceSettings()).thenReturn(balanceSettingsMock);
    when(accountSettingsMock.assetScale()).thenReturn(2); // Hard-coded since this is not being tested in this class.

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
      assertThat(e.getMessage()).isEqualTo("destinationAccountSettings must not be null");
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
    when(accountSettingsMock.accountId()).thenReturn(accountId);
    balanceTracker.updateBalanceForFulfill(accountSettingsMock, ONE);

    final AccountBalance loadedBalance = balanceTracker.balance(accountId);
    assertThat(loadedBalance.clearingBalance()).isEqualTo(ONE);
    assertThat(loadedBalance.prepaidAmount()).isEqualTo(ZERO);
    assertThat(loadedBalance.netBalance().longValue()).isEqualTo(ONE);
  }

  /**
   * Verify the correct clearingBalance when the `min_balance` is greater than the
   */
  @Test
  public void computeSettlementQuantityWithParamterizedValues() {
    this.initializeAccount(ACCOUNT_ID, this.existingClearingBalance, this.existingPrepaidBalance);

    assertThat(accountSettingsMock.balanceSettings().settleTo()).isEqualTo(settleTo);
    assertThat(accountSettingsMock.balanceSettings().settleThreshold()).isEqualTo(settleThreshold);

    BalanceTracker.UpdateBalanceForFulfillResponse prepareResponse =
      balanceTracker.updateBalanceForFulfill(accountSettingsMock, prepareAmount);

    assertThat(prepareResponse.clearingAmountToSettle()).isEqualTo(expectedClearingAmountToSettle);
  }

}
