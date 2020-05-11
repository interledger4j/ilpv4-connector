package org.interledger.connector.opa.model;

import org.immutables.value.Value;

@Value.Immutable
public interface PaymentDetails {

  static ImmutablePaymentDetails.Builder builder() {
    return ImmutablePaymentDetails.builder();
  }

}
