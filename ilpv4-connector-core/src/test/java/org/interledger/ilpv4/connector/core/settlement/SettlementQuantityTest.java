package org.interledger.ilpv4.connector.core.settlement;


import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link SettlementQuantity}.
 */
public class SettlementQuantityTest {

  @Test(expected = IllegalStateException.class)
  public void builderWithoutAmount() {
    try {
      SettlementQuantity.builder()
        .scale(3)
        .build();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(),
        is("Cannot build SettlementQuantity, some of required attributes are not set [amount]"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void builderWithoutScale() {
    try {
      SettlementQuantity.builder()
        .amount(BigInteger.ONE)
        .build();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is(
        "Cannot build SettlementQuantity, some of required attributes are not set [scale]")
      );
      throw e;
    }
  }

  @Test
  public void builder() {
    SettlementQuantity settlementQuantity = SettlementQuantity.builder()
      .amount(BigInteger.TEN)
      .scale(3)
      .build();

    assertThat(settlementQuantity.amount(), is(BigInteger.TEN));
    assertThat(settlementQuantity.scale(), is(3));
  }

  @Test
  public void allZeros() {
    SettlementQuantity settlementQuantity = SettlementQuantity.builder()
      .amount(BigInteger.ZERO)
      .scale(0)
      .build();

    assertThat(settlementQuantity.amount(), is(BigInteger.ZERO));
    assertThat(settlementQuantity.scale(), is(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeScale() {
    try {
      SettlementQuantity.builder()
        .amount(BigInteger.TEN)
        .scale(-3)
        .build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("scale must not be negative"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeAmount() {
    try {
      SettlementQuantity.builder()
        .amount(BigInteger.TEN.negate())
        .scale(2)
        .build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("amount must not be negative"));
      throw e;
    }
  }
}
