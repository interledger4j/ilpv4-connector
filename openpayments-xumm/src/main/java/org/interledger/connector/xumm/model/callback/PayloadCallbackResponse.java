package org.interledger.connector.xumm.model.callback;

import org.interledger.connector.xumm.model.ReturnUrl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePayloadCallbackResponse.class)
@JsonDeserialize(as = ImmutablePayloadCallbackResponse.class)
public interface PayloadCallbackResponse {

  static ImmutablePayloadCallbackResponse.Builder builder() {
    return ImmutablePayloadCallbackResponse.builder();
  }

  @JsonProperty("payload_uuidv4")
  String payloadUuidV4();

  @JsonProperty("reference_call_uuidv4")
  String referenceCallUuidV4();

  boolean signed();

  @JsonProperty("user_token")
  boolean userToken();

  @JsonProperty("return_url")
  Optional<ReturnUrl> returnUrl();

}
