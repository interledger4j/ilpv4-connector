package org.interledger.connector.opa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMemoWrapper.class)
@JsonDeserialize(as = ImmutableMemoWrapper.class)
public interface MemoWrapper {

  static ImmutableMemoWrapper.Builder builder() {
    return ImmutableMemoWrapper.builder();
  }

  @JsonProperty("Memo")
  Memo memo();
}
