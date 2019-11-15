package org.interledger.connector.settlement.client;

import org.interledger.connector.accounts.SettlementEngineAccountId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * An object for modeling a POST request to the `/accounts` endpoint of a Settlement Engine.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableCreateSettlementAccountRequest.class)
@JsonDeserialize(as = ImmutableCreateSettlementAccountRequest.class)
public interface CreateSettlementAccountRequest {

  static ImmutableCreateSettlementAccountRequest.Builder builder() {
    return ImmutableCreateSettlementAccountRequest.builder();
  }

  /**
   * The connector's account scale.
   *
   * @return An integer representing the scale used by the settlement engine.
   */
  @JsonProperty("id")
  Optional<SettlementEngineAccountId> requestedSettlementAccountId();
}
