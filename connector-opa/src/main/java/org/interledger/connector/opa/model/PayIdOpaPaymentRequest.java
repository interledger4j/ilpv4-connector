package org.interledger.connector.opa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutablePayIdOpaPaymentRequest.class)
@JsonSerialize(as = ImmutablePayIdOpaPaymentRequest.class)
public interface PayIdOpaPaymentRequest {

  static ImmutablePayIdOpaPaymentRequest.Builder builder() {
    return ImmutablePayIdOpaPaymentRequest.builder();
  }

  String destinationPayId();
  UnsignedLong amount();

}
