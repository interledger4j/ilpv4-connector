package org.interledger.connector.xumm.model.callback;

import org.interledger.connector.xumm.model.CustomMeta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePayloadCallback.class)
@JsonDeserialize(as = ImmutablePayloadCallback.class)
public interface PayloadCallback {

  static ImmutablePayloadCallback.Builder builder() {
    return ImmutablePayloadCallback.builder();
  }

  Meta meta();

  @JsonProperty("custom_meta")
  CustomMeta customMeta();

  PayloadCallbackResponse payloadResponse();

  UserToken userToken();

}
