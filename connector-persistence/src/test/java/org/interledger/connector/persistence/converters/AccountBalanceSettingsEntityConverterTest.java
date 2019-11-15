package org.interledger.connector.persistence.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.persistence.entities.AccountBalanceSettingsEntity;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@linkn AccountBalanceSettingsConverter}.
 */
public class AccountBalanceSettingsEntityConverterTest {

  private AccountBalanceSettingsEntityConverter converter;

  @Before
  public void setUp() {
    this.converter = new AccountBalanceSettingsEntityConverter();
  }

  @Test
  public void convert() {
    final AccountBalanceSettings accountBalanceSettings = AccountBalanceSettings.builder()
      .settleThreshold(100L)
      .settleTo(1L)
      .minBalance(50L)
      .build();
    final AccountBalanceSettingsEntity entity = new AccountBalanceSettingsEntity(accountBalanceSettings);

    final AccountBalanceSettings actual = converter.convert(entity);

    assertThat(actual.settleThreshold()).isEqualTo(accountBalanceSettings.settleThreshold());
    assertThat(actual.settleTo()).isEqualTo(accountBalanceSettings.settleTo());
    assertThat(actual.minBalance()).isEqualTo(accountBalanceSettings.minBalance());
  }
}
