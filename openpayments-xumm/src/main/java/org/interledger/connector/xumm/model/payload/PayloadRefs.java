package org.interledger.connector.xumm.model.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value.Immutable;

@Immutable
@JsonSerialize(as = ImmutablePayloadRefs.class)
@JsonDeserialize(as = ImmutablePayloadRefs.class)
public interface PayloadRefs {

  static ImmutablePayloadRefs.Builder builder() {
    return ImmutablePayloadRefs.builder();
  }

  @JsonProperty("qr_png")
  HttpUrl qrPng();

  @JsonProperty("qr_matrix")
  HttpUrl qrMatrx();

}
