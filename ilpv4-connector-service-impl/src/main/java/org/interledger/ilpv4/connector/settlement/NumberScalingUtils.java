package org.interledger.ilpv4.connector.settlement;

import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Utilities for normalizing scaled account values.
 */
public class NumberScalingUtils {

  /**
   * Translate a {@link SettlementQuantity} with a given scale into a new settlementQuantity with a scale of {@code
   * destinationScale}.
   *
   * @param settlementQuantity A {@link SettlementQuantity} to translate into a new scaled amount.
   * @param destinationScale   TODO: Take definition of `scale` from RFC once it's finalized.
   *
   * @return A new {@link SettlementQuantity} with scale of {@code destinationScale}.
   */
  public static SettlementQuantity translate(final SettlementQuantity settlementQuantity, final int destinationScale) {

    Objects.requireNonNull(settlementQuantity, "SettlementQuantity must not be null");

    // The difference between the two `scale` values
    final int scaleDifference = destinationScale - settlementQuantity.scale();

    // Unsigned long....
    final BigInteger settlementQuantityAmount = BigInteger.valueOf(settlementQuantity.amount());


    final BigInteger newAmount = scaleDifference > 0 ?
      // value * (10^scaleDifference)
      settlementQuantityAmount.multiply(BigInteger.TEN.pow(scaleDifference)) :
      // value / (10^-scaleDifference))
      settlementQuantityAmount.divide((BigInteger.TEN.pow(scaleDifference * -1)));

    // TODO: If overflow, should we return a quantity of 0 instead of blowing up?
    return SettlementQuantity.builder()
      .amount(newAmount.longValueExact())
      .scale(destinationScale)
      .build();
  }

}
