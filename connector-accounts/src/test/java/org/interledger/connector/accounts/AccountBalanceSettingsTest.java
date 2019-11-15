package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link AccountBalanceSettings}.
 */
public class AccountBalanceSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testDefaultSettings() {
    final AccountBalanceSettings accountBalanceSettings = AccountBalanceSettings.builder().build();

    assertThat(accountBalanceSettings.settleThreshold().isPresent()).isFalse();
    assertThat(accountBalanceSettings.settleTo()).isEqualTo(0L);
    assertThat(accountBalanceSettings.minBalance().isPresent()).isFalse();
  }

  @Test
  public void testFullyPopulated() {
    final AccountBalanceSettings accountBalanceSettings = AccountBalanceSettings.builder()
      .settleThreshold(Long.MAX_VALUE)
      .settleTo(1L)
      .minBalance(9L)
      .build();

    assertThat(accountBalanceSettings.settleThreshold().get()).isEqualTo(Long.MAX_VALUE);
    assertThat(accountBalanceSettings.settleTo()).isEqualTo(1L);
    assertThat(accountBalanceSettings.minBalance().get()).isEqualTo(9L);
  }

  @Test
  public void testWhenSettleThresholdLessThanSettleTo() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("settleThreshold must be greater than or equal to the settleTo");

    AccountBalanceSettings.builder()
      .settleThreshold(0L)
      .settleTo(1L)
      .build();
  }

  @Test
  public void testWhenSettleThresholdEqualToSettleTo() {
    final AccountBalanceSettings accountBalanceSettings = AccountBalanceSettings.builder()
      .settleThreshold(1L)
      .settleTo(1L)
      .build();

    assertThat(accountBalanceSettings.settleThreshold().get()).isEqualTo(1L);
    assertThat(accountBalanceSettings.settleTo()).isEqualTo(1L);
    assertThat(accountBalanceSettings.minBalance().isPresent()).isFalse();
  }

  @Test
  public void testWhenSettleThresholdGreaterThanSettleTo() {
    final AccountBalanceSettings accountBalanceSettings = AccountBalanceSettings.builder()
      .settleThreshold(10L)
      .settleTo(1L)
      .build();

    assertThat(accountBalanceSettings.settleThreshold().get()).isEqualTo(10L);
    assertThat(accountBalanceSettings.settleTo()).isEqualTo(1L);
    assertThat(accountBalanceSettings.minBalance().isPresent()).isFalse();
  }
}
