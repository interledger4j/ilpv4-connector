package org.interledger.ilpv4.connector.settlement;

import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link NumberScalingUtils}.
 */
@RunWith(Parameterized.class)
public class NumberScalingUtilsTest {

  private static final BigInteger ONE_HUNDRED = BigInteger.valueOf(100L);

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
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
        SettlementQuantity.builder().scale(2).amount(ONE_HUNDRED).build(),
      },

      new Object[]{
        "Convert 2 Dollars (scale: 0) to cents (scale: 2)",
        SettlementQuantity.builder().scale(0).amount(BigInteger.valueOf(2)).build(),
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(200)).build(),
      },

      new Object[]{
        "Convert Dollars (scale: 0) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert 99 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(99)).build(),
        SettlementQuantity.builder().scale(0).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 100 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(ONE_HUNDRED).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert 101 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(101)).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert 501 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(501)).build(),
        SettlementQuantity.builder().scale(0).amount(BigInteger.valueOf(5)).build(),
      },

      new Object[]{
        "Convert Dime-Dollars (scale: 1) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(1).amount(TEN).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert Dime-Dollars (scale: 1) to Milli-Dollars (scale: 6)",
        SettlementQuantity.builder().scale(1).amount(ONE).build(),
        SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(100000L)).build(),
      },

      ////////////
      // XRP (scale = 0)
      ////////////

      new Object[]{
        "Convert 100 Drops (scale: 6) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
        SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
      },

      new Object[]{
        "Convert 1 Drop (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(ONE).build(),
        SettlementQuantity.builder().scale(0).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 100 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
        SettlementQuantity.builder().scale(0).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 999 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
        SettlementQuantity.builder().scale(0).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 999999 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(999999L)).build(),
        SettlementQuantity.builder().scale(0).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 1M Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(1000000L)).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert 1 Milli-Drop (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(ONE).build(),
        SettlementQuantity.builder().scale(6).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 999 Milli-Drops (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(BigInteger.valueOf(999L)).build(),
        SettlementQuantity.builder().scale(6).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 1000 Milli-Drops (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(BigInteger.valueOf(1000L)).build(),
        SettlementQuantity.builder().scale(6).amount(ONE).build(),
      },

      //////////////////
      // Eth (Scale = 0)
      //////////////////

      new Object[]{
        "Convert 1 Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(ONE).build(),
        SettlementQuantity.builder().scale(9).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 1B Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(BigInteger.valueOf(1000000000L)).build(),
        SettlementQuantity.builder().scale(9).amount(ONE).build(),
      },

      new Object[]{
        "Convert 1B+1 Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(BigInteger.valueOf(1000000000)).build(),
        SettlementQuantity.builder().scale(9).amount(ONE).build(),
      },

      new Object[]{
        "Convert Wei (scale: 18) to Eth (scale: 0)",
        SettlementQuantity.builder().scale(18).amount(new BigInteger("1000000000000000000")).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert  Wei (scale: 18) to Eth (scale: 0)",
        SettlementQuantity.builder().scale(9).amount(ONE).build(),
        SettlementQuantity.builder().scale(18).amount(BigInteger.valueOf(1000000000L)).build(),
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
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(103L)).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert $0.99 to $0 Dollars (rounding down)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(99L)).build(),
        SettlementQuantity.builder().scale(0).amount(ZERO).build(),
      },

      new Object[]{
        "Convert 1,000,001 drops to 1 XRP (rounding down)",
        SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(1000000)).build(),
        SettlementQuantity.builder().scale(0).amount(ONE).build(),
      },

      new Object[]{
        "Convert 999,999 to $0 XRP (rounding down)",
        SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(999999L)).build(),
        SettlementQuantity.builder().scale(0).amount(ZERO).build(),
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
    assertThat(
      description, NumberScalingUtils.translate(
        sourceSettlementQuantity.amount(), sourceSettlementQuantity.scale(), expectedSettlementQuantity.scale()
      ),
      is(expectedSettlementQuantity.amount()));
  }
}
