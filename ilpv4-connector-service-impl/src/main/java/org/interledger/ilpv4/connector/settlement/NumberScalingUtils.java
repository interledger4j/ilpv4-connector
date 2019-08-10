package org.interledger.ilpv4.connector.settlement;

import com.google.common.base.Preconditions;
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
   *
   * @deprecated Should be replaced by a `translate` method that overtly accepts three values.
   */
  @Deprecated
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

  /**
   * Translate a {@link SettlementQuantity} with a given scale into a new settlementQuantity with a scale of {@code
   * destinationScale}.
   *
   * @param sourceAmount     A {@link BigInteger} representing the amount of units, in the clearing account's scale, to
   *                         attempt to settle.
   * @param sourceScale      An int representing a source scale.
   * @param destinationScale An int representing a destination scale
   *
   * @return A new {@link BigInteger} containing an amount scaled into the {@code destinationScale} units.
   *
   * @see "https://java-router.ilpv4.dev/overview/terminology#scale"
   */
  public static BigInteger translate(
    final BigInteger sourceAmount, final int sourceScale, final int destinationScale
  ) {
    Objects.requireNonNull(sourceAmount, "sourceAmount must not be null");
    Preconditions.checkArgument(sourceAmount.compareTo(BigInteger.ZERO) >= 0, "sourceAmount must be positive");
    Preconditions.checkArgument(sourceScale >= 0, "sourceScale must be positive");
    Preconditions.checkArgument(destinationScale >= 0, "destinationScale must be positive");

    // The difference between the two `scale` values
    final int scaleDifference = destinationScale - sourceScale;

    final BigInteger scaledAmount = scaleDifference > 0 ?
      // value * (10^scaleDifference)
      sourceAmount.multiply(BigInteger.TEN.pow(scaleDifference)) :
      // value / (10^-scaleDifference))
      sourceAmount.divide((BigInteger.TEN.pow(scaleDifference * -1)));

    return scaledAmount;
  }
}
