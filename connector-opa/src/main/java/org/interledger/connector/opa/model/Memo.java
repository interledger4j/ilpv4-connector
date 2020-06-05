package org.interledger.connector.opa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.io.BaseEncoding;
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

  static Memo decoded(Memo encoded) {
    return Memo.builder()
      .memoData(hexDecode(encoded.memoData()))
      .memoType(hexDecode(encoded.memoType()))
      .memoFormat(encoded.memoFormat().map(Memo::hexDecode))
      .build();
  }

  @JsonProperty("MemoFormat")
  Optional<String> memoFormat();

  @JsonProperty("MemoType")
  String memoType();

  @JsonProperty("MemoData")
  String memoData();

  static String hexDecode(String value) {
    try {
      return new String(BaseEncoding.base16().decode(value));
    } catch (IllegalArgumentException e) {
      // decoding failed possibly because already decoded
      return value;
    }
  }
}
