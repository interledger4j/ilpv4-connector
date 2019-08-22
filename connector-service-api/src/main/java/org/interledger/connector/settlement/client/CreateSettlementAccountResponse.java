package org.interledger.connector.settlement.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.settlement.client.ImmutableCreateSettlementAccountResponse;

/**
 * An object for modeling a response from the `/settlements` endpoint of a Settlement Engine.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableCreateSettlementAccountResponse.class)
@JsonDeserialize(as = ImmutableCreateSettlementAccountResponse.class)
public interface CreateSettlementAccountResponse {

  static ImmutableCreateSettlementAccountResponse.Builder builder() {
    return ImmutableCreateSettlementAccountResponse.builder();
  }

  /**
   * The settlement engine's scale.
   *
   * @return An integer representing the scale used by the settlement engine.
   */
  @JsonProperty("id")
  SettlementEngineAccountId settlementEngineAccountId();

}
