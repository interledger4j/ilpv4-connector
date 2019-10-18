package org.interledger.connector.core.settlement;


import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.math.BigInteger;

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
      assertThat(e.getMessage())
          .isEqualTo("Cannot build SettlementQuantity, some of required attributes are not set [amount]");
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
      assertThat(e.getMessage())
          .isEqualTo("Cannot build SettlementQuantity, some of required attributes are not set [scale]");
      throw e;
    }
  }

  @Test
  public void builder() {
    SettlementQuantity settlementQuantity = SettlementQuantity.builder()
      .amount(BigInteger.TEN)
      .scale(3)
      .build();

    assertThat(settlementQuantity.amount()).isEqualTo(BigInteger.TEN);
    assertThat(settlementQuantity.scale()).isEqualTo(3);
  }

  @Test
  public void allZeros() {
    SettlementQuantity settlementQuantity = SettlementQuantity.builder()
      .amount(BigInteger.ZERO)
      .scale(0)
      .build();

    assertThat(settlementQuantity.amount()).isEqualTo(BigInteger.ZERO);
    assertThat(settlementQuantity.scale()).isEqualTo(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeScale() {
    try {
      SettlementQuantity.builder()
        .amount(BigInteger.TEN)
        .scale(-3)
        .build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("scale must not be negative");
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
      assertThat(e.getMessage()).isEqualTo("amount must not be negative");
      throw e;
    }
  }

  @Test
  public void serialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    String serialized = objectMapper.writeValueAsString(SettlementQuantity.builder()
        .amount(BigInteger.TEN)
        .scale(3)
        .build());

    assertThat(serialized).isEqualTo("{\"amount\":\"10\",\"scale\":3}");
  }
}
