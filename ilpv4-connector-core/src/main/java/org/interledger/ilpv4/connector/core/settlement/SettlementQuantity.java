package org.interledger.ilpv4.connector.core.settlement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

/**
 * Represents an amount denominated in some unit of a particular fungible asset.
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
  long amount();

  /**
   * TODO: Clarify definition in this Javadoc once RFC is finalized.
   *
   * @return
   */
  int scale();

  @Value.Check
  default SettlementQuantity check() {
    Preconditions.checkArgument(amount() >= 0, "amount must not be negative");

    // Scale can theoretically be negative, though we don't have any uses-cases at present.
    // See https://github.com/interledger/rfcs/pull/536/files#r296461291
    //Preconditions.checkArgument(scale() >= 0, "scale must not be negative");

    return this;
  }
}
