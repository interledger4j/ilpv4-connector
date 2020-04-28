package org.interledger.connector.opa.xrpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableXrplMessage.class)
@JsonDeserialize(as = ImmutableXrplMessage.class)
public interface XrplMessage {

  @Nullable
  Boolean validated();

  @JsonProperty("engine_result")
  @Nullable
  String engineResult();

  String type();

  @Nullable
  XrplTransaction transaction();

  static ImmutableXrplMessage.Builder builder() {
    return ImmutableXrplMessage.builder();
  }

  @Value.Derived
  default boolean isSuccessfulTransaction() {
    return "tesSUCCESS".equals(engineResult()) && "transaction".equals(type());
  }

}
