package org.interledger.openpayments.xrpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMemo.class)
@JsonDeserialize(as = ImmutableMemo.class)
public interface Memo {

  static ImmutableMemo.Builder builder() {
    return ImmutableMemo.builder();
  }

  @JsonProperty("MemoFormat")
  String memoFormat();

  @JsonProperty("MemoType")
  String memoType();

  @JsonProperty("MemoData")
  String memoData();
}
