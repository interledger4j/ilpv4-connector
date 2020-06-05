package org.interledger.connector.payid;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePayIdAddressDetails.class)
@JsonDeserialize(as = ImmutablePayIdAddressDetails.class)
public interface PayIdAddressDetails {

  static ImmutablePayIdAddressDetails.Builder builder() {
    return ImmutablePayIdAddressDetails.builder();
  }

  String address();

}
