package org.interledger.connector.opa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableXrpPayment.class)
@JsonDeserialize(as = ImmutableXrpPayment.class)
public interface XrpPayment {

  static ImmutableXrpPayment.Builder builder() {
    return ImmutableXrpPayment.builder();
  }

  XrpPaymentDetails paymentDetails();

  UnsignedLong amount();

}
