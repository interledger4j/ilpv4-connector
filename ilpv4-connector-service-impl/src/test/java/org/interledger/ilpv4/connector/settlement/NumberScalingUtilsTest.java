package org.interledger.ilpv4.connector.settlement;

import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link NumberScalingUtils}.
 */
@RunWith(Parameterized.class)
public class NumberScalingUtilsTest {

  private String description;
  private SettlementQuantity sourceSettlementQuantity;
  private SettlementQuantity expectedSettlementQuantity;

  public NumberScalingUtilsTest(
    final String description,
    final SettlementQuantity sourceSettlementQuantity,
    final SettlementQuantity expectedSettlementQuantity
  ) {
    this.description = Objects.requireNonNull(description);
    this.sourceSettlementQuantity = Objects.requireNonNull(sourceSettlementQuantity);
    this.expectedSettlementQuantity = Objects.requireNonNull(expectedSettlementQuantity);
  }

  @Parameterized.Parameters
  @SuppressWarnings("PMD")
  public static Collection<Object[]> testValue() {
    return Arrays.asList(

      /////////////////////
      // USD (scale = 0)
      /////////////////////

      new Object[]{
        "Convert 1 Dollars (scale: 0) to cents (scale: 2)",
        SettlementQuantity.builder().scale(0).amount(1L).build(),
        SettlementQuantity.builder().scale(2).amount(100L).build(),
      },

      new Object[]{
        "Convert 2 Dollars (scale: 0) to cents (scale: 2)",
        SettlementQuantity.builder().scale(0).amount(BigInteger.valueOf(2).longValueExact()).build(),
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(200).longValueExact()).build(),
      },

      new Object[]{
        "Convert Dollars (scale: 0) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(0).amount(1L).build(),
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 99 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(99).longValueExact()).build(),
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 100 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(100L).build(),
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 101 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(101).longValueExact()).build(),
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 501 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(501).longValueExact()).build(),
        SettlementQuantity.builder().scale(0).amount(BigInteger.valueOf(5).longValueExact()).build(),
      },

      new Object[]{
        "Convert Dime-Dollars (scale: 1) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(1).amount(10L).build(),
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert Dime-Dollars (scale: 1) to Milli-Dollars (scale: 6)",
        SettlementQuantity.builder().scale(1).amount(1L).build(),
        SettlementQuantity.builder().scale(6).amount(100000L).build(),
      },

      ////////////
      // XRP (scale = 0)
      ////////////

      new Object[]{
        "Convert 100 Drops (scale: 6) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(6).amount(100L).build(),
        SettlementQuantity.builder().scale(6).amount(100L).build(),
      },

      new Object[]{
        "Convert 1 Drop (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(1L).build(),
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 100 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(100L).build(),
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 999 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(100L).build(),
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 999999 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(999999L).build(),
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 1M Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(1000000L).build(),
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 1 Milli-Drop (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(1L).build(),
        SettlementQuantity.builder().scale(6).amount(0L).build(),
      },

      new Object[]{
        "Convert 999 Milli-Drops (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(999L).build(),
        SettlementQuantity.builder().scale(6).amount(0L).build(),
      },

      new Object[]{
        "Convert 1000 Milli-Drops (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(1000L).build(),
        SettlementQuantity.builder().scale(6).amount(1L).build(),
      },

      //////////////////
      // Eth (Scale = 0)
      //////////////////

      new Object[]{
        "Convert 1 Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(1L).build(),
        SettlementQuantity.builder().scale(9).amount(0L).build(),
      },

      new Object[]{
        "Convert 1B Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(1000000000L).build(),
        SettlementQuantity.builder().scale(9).amount(1L).build(),
      },

      new Object[]{
        "Convert 1B+1 Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(1000000001L).build(),
        SettlementQuantity.builder().scale(9).amount(1L).build(),
      },

      new Object[]{
        "Convert Wei (scale: 18) to Eth (scale: 0)",
        SettlementQuantity.builder().scale(18).amount(new BigInteger("1000000000000000000").longValueExact()).build(),
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert  Wei (scale: 18) to Eth (scale: 0)",
        SettlementQuantity.builder().scale(9).amount(1L).build(),
        SettlementQuantity.builder().scale(18).amount(1000000000L).build(),
      },

      ////////////////
      // Rounding Validations
      // Number scaling utils MUST always round to the floor. The current implementation guarantees this in two ways.
      // First, if the difference between the destination and source currencies is positive, then the source amount will
      // be multiplied by (10^diff), which will always yield a whole number. If the difference between the destination
      // and source currencies is negative, then the source amount will be divided by (10^-diff), which might produce
      // a remainder. In these scenarios, the implementation will round to the "floor", producing a whole number and
      // a leftover that can be ignored (and processed later).
      ////////////////

      new Object[]{
        "Convert $1.03 to $1 Dollars (rounding down)",
        SettlementQuantity.builder().scale(2).amount(103L).build(),
        SettlementQuantity.builder().scale(0).amount(1).build(),
      },

      new Object[]{
        "Convert $0.99 to $0 Dollars (rounding down)",
        SettlementQuantity.builder().scale(2).amount(99L).build(),
        SettlementQuantity.builder().scale(0).amount(0).build(),
      },

      new Object[]{
        "Convert 1,000,001 drops to 1 XRP (rounding down)",
        SettlementQuantity.builder().scale(6).amount(1000001L).build(),
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 999,999 to $0 XRP (rounding down)",
        SettlementQuantity.builder().scale(6).amount(999999L).build(),
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      }

    );
  }

  @Test(expected = NullPointerException.class)
  public void translateWithNullSourceQuantity() {
    try {
      NumberScalingUtils.translate(null, 1, 2);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAmount must not be null"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void translateWithNegativeSourceScale() {
    try {
      NumberScalingUtils.translate(BigInteger.ZERO, -11, 2);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceScale must not be negative"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void translateWithNegativeDestinationScale() {
    try {
      NumberScalingUtils.translate(BigInteger.ZERO, 1, -1);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationScale must not be negative"));
      throw e;
    }
  }

  @Test
  public void translate() {
    assertThat(description, NumberScalingUtils.translate(
      BigInteger.valueOf(sourceSettlementQuantity.amount()), sourceSettlementQuantity.scale(),
      expectedSettlementQuantity.scale()), is(BigInteger.valueOf(expectedSettlementQuantity.amount())));
  }
}
