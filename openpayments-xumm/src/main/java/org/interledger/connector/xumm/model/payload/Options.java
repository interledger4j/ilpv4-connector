package org.interledger.connector.xumm.model.payload;

import org.interledger.connector.xumm.model.ReturnUrl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableOptions.class)
@JsonDeserialize(as = ImmutableOptions.class)
public interface Options {

  static ImmutableOptions.Builder builder() {
    return ImmutableOptions.builder();
  }

  boolean submit();

  @Value.Default
  default boolean multisign() {
    return false;
  }

  /**
   * After how many minutes should the payload expire
   *
   * @return
   */
  Optional<Integer> expire();

  @JsonProperty("return_url")
  Optional<ReturnUrl> returnUrl();


}
