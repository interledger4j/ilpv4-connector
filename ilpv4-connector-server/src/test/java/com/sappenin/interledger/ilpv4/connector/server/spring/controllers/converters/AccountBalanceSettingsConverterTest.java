package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.ilpv4.connector.persistence.entities.AccountBalanceSettingsEntity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for {@linkn AccountBalanceSettingsConverter}.
 */
public class AccountBalanceSettingsConverterTest {

  private AccountBalanceSettingsConverter converter;

  @Before
  public void setUp() {
    this.converter = new AccountBalanceSettingsConverter();
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

    assertThat(actual.getSettleThreshold(), is(accountBalanceSettings.getSettleThreshold()));
    assertThat(actual.getSettleTo(), is(accountBalanceSettings.getSettleTo()));
    assertThat(actual.getMinBalance(), is(accountBalanceSettings.getMinBalance()));
  }
}
