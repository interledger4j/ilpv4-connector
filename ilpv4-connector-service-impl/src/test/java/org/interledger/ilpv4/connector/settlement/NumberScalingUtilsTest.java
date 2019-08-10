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
  private int destinationScale;
  private SettlementQuantity expectedSettlementQuantity;

  public NumberScalingUtilsTest(
    final String description, SettlementQuantity sourceSettlementQuantity, int destinationScale, SettlementQuantity expectedSettlementQuantity
  ) {
    this.description = Objects.requireNonNull(description);
    this.sourceSettlementQuantity = Objects.requireNonNull(sourceSettlementQuantity);
    this.destinationScale = destinationScale;
    this.expectedSettlementQuantity = Objects.requireNonNull(expectedSettlementQuantity);
  }

  // TODO: Add tests that excercise a loss of precision during the scaling operation, and ensure that the scaled
  //  amount is _always_ rounded down.
  // See https://github.com/sappenin/java-ilpv4-connector/issues/224

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
        2,
        SettlementQuantity.builder().scale(2).amount(100L).build(),
      },

      new Object[]{
        "Convert 2 Dollars (scale: 0) to cents (scale: 2)",
        SettlementQuantity.builder().scale(0).amount(BigInteger.valueOf(2).longValueExact()).build(),
        2,
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(200).longValueExact()).build(),
      },

      new Object[]{
        "Convert Dollars (scale: 0) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(0).amount(1L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 99 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(99).longValueExact()).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 100 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(100L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 101 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(101).longValueExact()).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 501 Cents (scale: 2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(501).longValueExact()).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(BigInteger.valueOf(5).longValueExact()).build(),
      },

      new Object[]{
        "Convert Hecto-Dollars (scale: -2) to Dollars (scale: 0)",
        SettlementQuantity.builder().scale(-2).amount(1L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(100L).build(),
      },

      new Object[]{
        "Convert Hecto-Dollars (scale: -2) to Hecto-Dollars (scale: -2)",
        SettlementQuantity.builder().scale(-2).amount(1L).build(),
        -2,
        SettlementQuantity.builder().scale(-2).amount(1L).build(),
      },

      new Object[]{
        "Convert Hecto-Dollars (scale: -2) to Milli-Dollars (scale: 6)",
        SettlementQuantity.builder().scale(-2).amount(1L).build(),
        6,
        SettlementQuantity.builder().scale(6).amount(100000000L).build(),
      },

      new Object[]{
        "Convert 1 Dollar (scale: 0) to mega-dollars (scale: -6)",
        SettlementQuantity.builder().scale(0).amount(1L).build(),
        -6,
        SettlementQuantity.builder().scale(-6).amount(0L).build(),
      },

      ////////////
      // XRP (scale = 0)
      ////////////

      new Object[]{
        "Convert 100 Drops (scale: 6) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(6).amount(100L).build(),
        6,
        SettlementQuantity.builder().scale(6).amount(100L).build(),
      },

      new Object[]{
        "Convert 1 Drop (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(1L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 100 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(100L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 999 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(100L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 999999 Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(999999L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(0L).build(),
      },

      new Object[]{
        "Convert 1M Drops (scale: 6) to XRP (scale: 0)",
        SettlementQuantity.builder().scale(6).amount(1000000L).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert 1 Milli-Drop (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(1L).build(),
        6,
        SettlementQuantity.builder().scale(6).amount(0L).build(),
      },

      new Object[]{
        "Convert 999 Milli-Drops (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(999L).build(),
        6,
        SettlementQuantity.builder().scale(6).amount(0L).build(),
      },

      new Object[]{
        "Convert 1000 Milli-Drops (scale: 9) to Drops (scale: 6)",
        SettlementQuantity.builder().scale(9).amount(1000L).build(),
        6,
        SettlementQuantity.builder().scale(6).amount(1L).build(),
      },

      //////////////////
      // Eth (Scale = 0)
      //////////////////

      new Object[]{
        "Convert 1 Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(1L).build(),
        9,
        SettlementQuantity.builder().scale(9).amount(0L).build(),
      },

      new Object[]{
        "Convert 1B Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(1000000000L).build(),
        9,
        SettlementQuantity.builder().scale(9).amount(1L).build(),
      },

      new Object[]{
        "Convert 1B+1 Wei (scale: 18) to Gwei (scale: 9)",
        SettlementQuantity.builder().scale(18).amount(1000000001L).build(),
        9,
        SettlementQuantity.builder().scale(9).amount(1L).build(),
      },

      new Object[]{
        "Convert Wei (scale: 18) to Eth (scale: 0)",
        SettlementQuantity.builder().scale(18).amount(new BigInteger("1000000000000000000").longValueExact()).build(),
        0,
        SettlementQuantity.builder().scale(0).amount(1L).build(),
      },

      new Object[]{
        "Convert  Wei (scale: 18) to Eth (scale: 0)",
        SettlementQuantity.builder().scale(9).amount(1L).build(),
        18,
        SettlementQuantity.builder().scale(18).amount(1000000000L).build(),
      }
    );
  }

  @Test(expected = NullPointerException.class)
  public void translateWithNullSourceQuantity() {
    try {
      NumberScalingUtils.translate(null, destinationScale);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("SettlementQuantity must not be null"));
      throw e;
    }
  }

  @Test
  public void translate() {
    assertThat(description, NumberScalingUtils.translate(sourceSettlementQuantity, destinationScale), is(
      expectedSettlementQuantity));
  }
}
