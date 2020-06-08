package org.interledger.connector.xumm.model.payload;

import org.interledger.connector.xumm.model.CustomMeta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePayload.class)
@JsonDeserialize(as = ImmutablePayload.class)
public interface Payload {

  static ImmutablePayload.Builder builder() {
    return ImmutablePayload.builder();
  }

  InnerPayload payload();

  Response response();

  @JsonProperty("custom_meta")
  CustomMeta customMeta();


  @Value.Immutable
  @JsonSerialize(as = ImmutableInnerPayload.class)
  @JsonDeserialize(as = ImmutableInnerPayload.class)
  interface InnerPayload {

    static ImmutableInnerPayload.Builder builder() {
      return ImmutableInnerPayload.builder();
    }

    @JsonProperty("tx_destination")
    String destination();

    @JsonProperty("tx_destination_tag")
    Optional<Integer> destinationTag();

    @JsonProperty("request_json")
    TxJson requestJson();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableResponse.class)
  @JsonDeserialize(as = ImmutableResponse.class)
  interface Response {

    static ImmutableResponse.Builder builder() {
      return ImmutableResponse.builder();
    }

    String account();
    String txid();
    @JsonProperty("resolved_at")
    Instant resolvedAt();
  }

}
