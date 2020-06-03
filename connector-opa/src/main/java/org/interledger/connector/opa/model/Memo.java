package org.interledger.connector.opa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableMemo.class)
@JsonDeserialize(as = ImmutableMemo.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface Memo {

  static ImmutableMemo.Builder builder() {
    return ImmutableMemo.builder();
  }

  @JsonProperty("MemoFormat")
  Optional<String> memoFormat();

  @JsonProperty("MemoType")
  String memoType();

  @JsonProperty("MemoData")
  String memoData();
}
