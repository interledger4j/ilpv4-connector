package org.interledger.connector.opa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableXrpPaymentDetails.class)
@JsonDeserialize(as = ImmutableXrpPaymentDetails.class)
public interface XrpPaymentDetails {

  static ImmutableXrpPaymentDetails.Builder builder() {
    return ImmutableXrpPaymentDetails.builder();
  }

  String address();

  Integer destinationTag();

}
