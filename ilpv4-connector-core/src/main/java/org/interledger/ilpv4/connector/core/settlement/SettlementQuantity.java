package org.interledger.ilpv4.connector.core.settlement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

/**
 * Models the `Quantity` JSON object as defined in the Settlement Engine RFC.
 *
 * TODO: Fix this URL once the RFC is published.
 *
 * @see "TBD"
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSettlementQuantity.class)
@JsonDeserialize(as = ImmutableSettlementQuantity.class)
public interface SettlementQuantity {

  static ImmutableSettlementQuantity.Builder builder() {
    return ImmutableSettlementQuantity.builder();
  }

  /**
   * <p>The non-negative number of units in this quantity, represented as a number between 0 and (2^64 - 1). Note that
   * this value fits inside of a 64-bit unsigned integer.</p>
   *
   * <p>Note that this value is typed as a {@link String} per the RFC in order to ensure no precision is lost on
   * platforms that don't natively support full precision 64-bit integers.
   *
   * @return A {@link String} representing the amount.
   */
  // TODO: Make a BigInteger per this RFC comment: https://github.com/interledger/rfcs/pull/536/files#r310810982
  long amount();

  /**
   * <p>The difference in orders of magnitude between a **standard unit** and a corresponding **fractional unit**. More
   * formally, the asset scale is a non-negative integer (0, 1, 2, …) such that one **standard unit** equals
   * `10^(-scale)` of a corresponding **fractional unit**. If the fractional unit equals the standard unit, then the
   * asset scale is 0.</p>
   *
   * <p>For example, if an asset is denominated in U.S. Dollars, then the standard unit will be a "dollar." To
   * represent a fractional unit such as "cents", a scale of 2 must be used.</p>
   *
   * @return The scale, as defined by the Settlement Engine RFC.
   */
  int scale();

  @Value.Check
  default SettlementQuantity check() {
    Preconditions.checkArgument(amount() >= 0, "amount must not be negative");

    // Scale can theoretically be negative, though we don't have any uses-cases at present. Thus we enforce it be
    // non-negative.
    // See https://github.com/interledger/rfcs/pull/536/files#r296461291
    Preconditions.checkArgument(scale() >= 0, "scale must not be negative");

    return this;
  }
}
