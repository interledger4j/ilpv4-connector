package org.interledger.connector.settlement;

import com.google.common.base.Preconditions;
import org.interledger.connector.core.settlement.SettlementQuantity;

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
   * @param sourceAmount     A {@link BigInteger} representing the amount of units, in the clearing account's scale, to
   *                         attempt to settle.
   * @param sourceScale      An int representing a source scale.
   * @param destinationScale An int representing a destination scale
   *
   * @return A new {@link BigInteger} containing an amount scaled into the {@code destinationScale} units. Note that if
   * there are any remainder units after performing the conversion, these will not be present in this return value (they
   * will be left in the balance tracker to be dealt with in future adjustments).
   *
   * @see "https://java-connector.ilpv4.dev/overview/terminology#scale"
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
      // value * (10^scaleDifference), always rounds to floor via BigInteger
      sourceAmount.multiply(BigInteger.TEN.pow(scaleDifference)) :
      // value / (10^-scaleDifference))
      sourceAmount.divide((BigInteger.TEN.pow(scaleDifference * -1)));

    return scaledAmount;
  }
}
