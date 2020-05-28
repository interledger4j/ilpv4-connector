package org.interledger.openpayments.events;

import org.interledger.connector.opa.model.Denomination;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableXrplTransaction.class)
@JsonDeserialize(as = ImmutableXrplTransaction.class)
public interface XrplTransaction {

  @JsonProperty("Account")
  String account();

  @JsonProperty("SourceTag")
  @Nullable
  String sourceTag();

  @JsonProperty("Destination")
  String destination();

  @Nullable
  @JsonProperty("DestinationTag")
  Integer destinationTag();

  @JsonProperty("Amount")
  UnsignedLong amount();

  String hash();

  @JsonProperty("Memos")
  @Nullable
  List<MemoWrapper> memos();

  @Value.Default
  default Instant createdAt() {
    return Instant.now();
  };

  @Value.Default
  default Instant modifiedAt() {
    return Instant.now();
  };

  @Value.Default
  default Denomination denomination() {
    return Denomination.builder()
      .assetScale((short) 6)
      .assetCode("XRP")
      .build();
  }

  static ImmutableXrplTransaction.Builder builder() {
    return ImmutableXrplTransaction.builder();
  }

}
