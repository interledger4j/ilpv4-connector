package org.interledger.connector.javax.money.providers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import javax.money.Monetary;

public class MonetaryTest {

  @Test
  public void getCurrency() {
    assertThat(Monetary.getCurrency("XRP").getDefaultFractionDigits()).isEqualTo(3);
    assertThat(Monetary.getCurrency("ETH").getDefaultFractionDigits()).isEqualTo(9);
  }

}
