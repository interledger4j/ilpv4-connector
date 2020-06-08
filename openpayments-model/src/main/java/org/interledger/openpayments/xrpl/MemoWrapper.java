package org.interledger.openpayments.xrpl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMemoWrapper.class)
@JsonDeserialize(as = ImmutableMemoWrapper.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface MemoWrapper {

  static ImmutableMemoWrapper.Builder builder() {
    return ImmutableMemoWrapper.builder();
  }

  @JsonProperty("Memo")
  Memo memo();
}
