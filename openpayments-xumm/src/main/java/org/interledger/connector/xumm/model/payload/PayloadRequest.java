package org.interledger.connector.xumm.model.payload;

import org.interledger.connector.xumm.model.CustomMeta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePayloadRequest.class)
@JsonDeserialize(as = ImmutablePayloadRequest.class)
public interface PayloadRequest {

  static ImmutablePayloadRequest.Builder builder() {
    return ImmutablePayloadRequest.builder();
  }

  TxJson txjson();

  @JsonProperty("user_token")
  Optional<String> userToken();

  @JsonProperty("custom_meta")
  Optional<CustomMeta> customMeta();

  Optional<Options> options();

}
