package com.sappenin.interledger.ilpv4.connector.balances;

import org.interledger.connector.accounts.AccountId;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tess for {@link AccountBalance}.
 */
public class AccountBalanceTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("123");

  @Test(expected = IllegalStateException.class)
  public void builderWithAccountIdUnSet() {
    try {
      AccountBalance.builder()
        .clearingBalance(0L)
        .prepaidAmount(0L)
        .build();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(),
        is("Cannot build AccountBalance, some of required attributes are not set [accountId]"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void builderWithClearingBalanceUnSet() {
    try {
      AccountBalance.builder()
        .accountId(ACCOUNT_ID)
        .prepaidAmount(0L)
        .build();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(),
        is("Cannot build AccountBalance, some of required attributes are not set [clearingBalance]"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void builderWithPrepaidAmountUnSet() {
    try {
      AccountBalance.builder()
        .accountId(ACCOUNT_ID)
        .clearingBalance(0L)
        .build();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(),
        is("Cannot build AccountBalance, some of required attributes are not set [prepaidAmount]"));
      throw e;
    }
  }

  @Test
  public void validValues() {
    final AccountBalance balance = AccountBalance.builder()
      .accountId(ACCOUNT_ID)
      .clearingBalance(5L)
      .prepaidAmount(25L)
      .build();

    assertThat(balance.accountId(), is(ACCOUNT_ID));
    assertThat(balance.netBalance(), is(BigInteger.valueOf(30L)));
    assertThat(balance.clearingBalance(), is(5L));
    assertThat(balance.prepaidAmount(), is(25L));
  }
}
