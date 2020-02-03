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
   * An optionally-present {@link SettlementEngineAccountId} that the Connector can use as a hint for the Settlement
   * Engine. If unspecified, the Settlement Engine will create a new account identifier and return it to the Connector.
   *
   * @return An optionally-present {@link SettlementEngineAccountId}.
   */
  @JsonProperty("id")
  Optional<SettlementEngineAccountId> requestedSettlementAccountId();
}
