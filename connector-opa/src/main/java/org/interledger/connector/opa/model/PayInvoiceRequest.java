package org.interledger.connector.opa.model;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

/**
 * Request object included in a request to pay an invoice.
 */
@Value.Immutable
public interface PayInvoiceRequest {

  static ImmutablePayInvoiceRequest.Builder builder() {
    return ImmutablePayInvoiceRequest.builder();
  }

  /**
   * The amount that should be paid.
   *
   * Defaults to {@link UnsignedLong#MAX_VALUE}, since the actual value paid will be the minimum of the amount
   * left to pay and this amount.
   *
   * @return An {@link UnsignedLong} representing the amount of an {@link Invoice} to pay.
   */
  @Value.Default
  default UnsignedLong amount() {
    return UnsignedLong.MAX_VALUE;
  };

}
