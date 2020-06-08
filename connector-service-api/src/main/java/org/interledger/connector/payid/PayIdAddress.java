package org.interledger.connector.payid;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePayIdAddress.class)
@JsonDeserialize(as = ImmutablePayIdAddress.class)
public interface PayIdAddress {

  static ImmutablePayIdAddress.Builder builder() {
    return ImmutablePayIdAddress.builder();
  }

  String paymentNetwork();

  String environment();

  String addressDetailsType();

  PayIdAddressDetails addressDetails();

}
