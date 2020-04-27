package org.interledger.connector.opa.xrpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableXrplTransaction.class)
@JsonDeserialize(as = ImmutableXrplTransaction.class)
public interface XrplTransaction {

  @JsonProperty("Account")
  String account();

  @JsonProperty("Destination")
  String destination();

  @Nullable
  @JsonProperty("DestinationTag")
  Integer destinationTag();

  @JsonProperty("Amount")
  UnsignedLong amount();

  String hash();

  static ImmutableXrplTransaction.Builder builder() {
    return ImmutableXrplTransaction.builder();
  }

}
