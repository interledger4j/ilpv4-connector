package org.interledger.ilpv4.connector.settlement;

import org.interledger.ilpv4.connector.core.settlement.Quantity;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Utilities for normalizing scaled account values.
 */
public class NumberScalingUtils {

  /**
   * Translate a {@link Quantity} with a given scale into a new quantity with a scale of {@code destinationScale}.
   *
   * @param quantity         A {@link Quantity} to translate into a new scaled amount.
   * @param destinationScale TODO: Take definition of `scale` from RFC once it's finalized.
   *
   * @return A new {@link Quantity} with scale of {@code destinationScale}.
   */
  public static Quantity translate(final Quantity quantity, final int destinationScale) {

    Objects.requireNonNull(quantity, "Quantity must not be null");

    // The difference between the two `scale` values
    final int scaleDifference = destinationScale - quantity.scale();

    final BigInteger newAmount = scaleDifference > 0 ?
      // value * (10^scaleDifference)
      quantity.amount().multiply(BigInteger.TEN.pow(scaleDifference)) :
      // value / (10^-scaleDifference))
      quantity.amount().divide((BigInteger.TEN.pow(scaleDifference * -1)));

    return Quantity.builder()
      .amount(newAmount)
      .scale(destinationScale)
      .build();
  }

}
