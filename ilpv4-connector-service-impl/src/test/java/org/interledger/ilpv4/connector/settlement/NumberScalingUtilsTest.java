package org.interledger.ilpv4.connector.settlement;

import org.interledger.ilpv4.connector.core.settlement.Quantity;
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
  private Quantity sourceQuantity;
  private int destinationScale;
  private Quantity expectedQuantity;

  public NumberScalingUtilsTest(
    final String description, Quantity sourceQuantity, int destinationScale, Quantity expectedQuantity
  ) {
    this.description = Objects.requireNonNull(description);
    this.sourceQuantity = Objects.requireNonNull(sourceQuantity);
    this.destinationScale = destinationScale;
    this.expectedQuantity = Objects.requireNonNull(expectedQuantity);
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
        Quantity.builder().scale(0).amount(BigInteger.ONE).build(),
        2,
        Quantity.builder().scale(2).amount(BigInteger.valueOf(100)).build(),
      },

      new Object[]{
        "Convert 2 Dollars (scale: 0) to cents (scale: 2)",
        Quantity.builder().scale(0).amount(BigInteger.valueOf(2)).build(),
        2,
        Quantity.builder().scale(2).amount(BigInteger.valueOf(200)).build(),
      },

      new Object[]{
        "Convert Dollars (scale: 0) to Dollars (scale: 0)",
        Quantity.builder().scale(0).amount(BigInteger.ONE).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert 99 Cents (scale: 2) to Dollars (scale: 0)",
        Quantity.builder().scale(2).amount(BigInteger.valueOf(99)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 100 Cents (scale: 2) to Dollars (scale: 0)",
        Quantity.builder().scale(2).amount(BigInteger.valueOf(100)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert 101 Cents (scale: 2) to Dollars (scale: 0)",
        Quantity.builder().scale(2).amount(BigInteger.valueOf(101)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert 501 Cents (scale: 2) to Dollars (scale: 0)",
        Quantity.builder().scale(2).amount(BigInteger.valueOf(501)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(5)).build(),
      },

      new Object[]{
        "Convert Hecto-Dollars (scale: -2) to Dollars (scale: 0)",
        Quantity.builder().scale(-2).amount(BigInteger.ONE).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(100)).build(),
      },

      new Object[]{
        "Convert Hecto-Dollars (scale: -2) to Hecto-Dollars (scale: -2)",
        Quantity.builder().scale(-2).amount(BigInteger.ONE).build(),
        -2,
        Quantity.builder().scale(-2).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert Hecto-Dollars (scale: -2) to Milli-Dollars (scale: 6)",
        Quantity.builder().scale(-2).amount(BigInteger.ONE).build(),
        6,
        Quantity.builder().scale(6).amount(BigInteger.valueOf(100000000)).build(),
      },

      new Object[]{
        "Convert 1 Dollar (scale: 0) to mega-dollars (scale: -6)",
        Quantity.builder().scale(0).amount(BigInteger.ONE).build(),
        -6,
        Quantity.builder().scale(-6).amount(BigInteger.valueOf(0)).build(),
      },

      ////////////
      // XRP (scale = 0)
      ////////////

      new Object[]{
        "Convert 100 Drops (scale: 6) to Drops (scale: 6)",
        Quantity.builder().scale(6).amount(BigInteger.valueOf(100)).build(),
        6,
        Quantity.builder().scale(6).amount(BigInteger.valueOf(100)).build(),
      },

      new Object[]{
        "Convert 1 Drop (scale: 6) to XRP (scale: 0)",
        Quantity.builder().scale(6).amount(BigInteger.ONE).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 100 Drops (scale: 6) to XRP (scale: 0)",
        Quantity.builder().scale(6).amount(BigInteger.valueOf(100)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 999 Drops (scale: 6) to XRP (scale: 0)",
        Quantity.builder().scale(6).amount(BigInteger.valueOf(100)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 999999 Drops (scale: 6) to XRP (scale: 0)",
        Quantity.builder().scale(6).amount(BigInteger.valueOf(999999)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 1M Drops (scale: 6) to XRP (scale: 0)",
        Quantity.builder().scale(6).amount(BigInteger.valueOf(1000000)).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert 1 Milli-Drop (scale: 9) to Drops (scale: 6)",
        Quantity.builder().scale(9).amount(BigInteger.ONE).build(),
        6,
        Quantity.builder().scale(6).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 999 Milli-Drops (scale: 9) to Drops (scale: 6)",
        Quantity.builder().scale(9).amount(BigInteger.valueOf(999)).build(),
        6,
        Quantity.builder().scale(6).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 1000 Milli-Drops (scale: 9) to Drops (scale: 6)",
        Quantity.builder().scale(9).amount(BigInteger.valueOf(1000)).build(),
        6,
        Quantity.builder().scale(6).amount(BigInteger.valueOf(1)).build(),
      },

      //////////////////
      // Eth (Scale = 0)
      //////////////////

      new Object[]{
        "Convert 1 Wei (scale: 18) to Gwei (scale: 9)",
        Quantity.builder().scale(18).amount(BigInteger.valueOf(1)).build(),
        9,
        Quantity.builder().scale(9).amount(BigInteger.valueOf(0)).build(),
      },

      new Object[]{
        "Convert 1B Wei (scale: 18) to Gwei (scale: 9)",
        Quantity.builder().scale(18).amount(BigInteger.valueOf(1000000000)).build(),
        9,
        Quantity.builder().scale(9).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert 1B+1 Wei (scale: 18) to Gwei (scale: 9)",
        Quantity.builder().scale(18).amount(BigInteger.valueOf(1000000001)).build(),
        9,
        Quantity.builder().scale(9).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert Wei (scale: 18) to Eth (scale: 0)",
        Quantity.builder().scale(18).amount(new BigInteger("1000000000000000000")).build(),
        0,
        Quantity.builder().scale(0).amount(BigInteger.valueOf(1)).build(),
      },

      new Object[]{
        "Convert  Wei (scale: 18) to Eth (scale: 0)",
        Quantity.builder().scale(9).amount(new BigInteger("1")).build(),
        18,
        Quantity.builder().scale(18).amount(BigInteger.valueOf(1000000000)).build(),
      }
    );
  }

  @Test(expected = NullPointerException.class)
  public void translateWithNullSourceQuantity() {
    try {
      NumberScalingUtils.translate(null, destinationScale);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("Quantity must not be null"));
      throw e;
    }
  }

  @Test
  public void translate() {
    assertThat(description, NumberScalingUtils.translate(sourceQuantity, destinationScale), is(expectedQuantity));
  }
}
