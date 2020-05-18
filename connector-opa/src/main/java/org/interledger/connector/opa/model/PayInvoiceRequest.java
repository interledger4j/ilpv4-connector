package org.interledger.connector.opa.model;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

@Value.Immutable
public interface PayInvoiceRequest {

  static ImmutablePayInvoiceRequest.Builder builder() {
    return ImmutablePayInvoiceRequest.builder();
  }

  @Value.Default
  default UnsignedLong amount() {
    return UnsignedLong.MAX_VALUE;
  };

}
