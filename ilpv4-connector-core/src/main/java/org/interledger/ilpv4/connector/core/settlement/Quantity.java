package org.interledger.ilpv4.connector.core.settlement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.math.BigInteger;

/**
 * Represents an amount denominated in some unit of a particular fungible asset.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableQuantity.class)
@JsonDeserialize(as = ImmutableQuantity.class)
public interface Quantity {

  static ImmutableQuantity.Builder builder() {
    return ImmutableQuantity.builder();
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
  BigInteger amount();

  /**
   * TODO: Clarify this. See https://github.com/interledger/rfcs/pull/536/files#diff-990a915317f4e35578edfa70631bd07dR142
   *
   * @return
   */
  int scale();

}
