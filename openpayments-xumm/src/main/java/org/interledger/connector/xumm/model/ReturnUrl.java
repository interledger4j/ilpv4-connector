package org.interledger.connector.xumm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableReturnUrl.class)
@JsonDeserialize(as = ImmutableReturnUrl.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface ReturnUrl {

  static ImmutableReturnUrl.Builder builder() {
    return ImmutableReturnUrl.builder();
  }

  Optional<String> app();

  Optional<String> web();

}
