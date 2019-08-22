package org.interledger.connector.settlement.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.connector.settlement.client.ImmutableInitiateSettlementResponse;

import java.math.BigInteger;

/**
 * An object for modeling a response from the `/settlements` endpoint of a Settlement Engine.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableInitiateSettlementResponse.class)
@JsonDeserialize(as = ImmutableInitiateSettlementResponse.class)
public interface InitiateSettlementResponse {

  static ImmutableInitiateSettlementResponse.Builder builder() {
    return ImmutableInitiateSettlementResponse.builder();
  }

  /**
   * The settlement engine's scale.
   *
   * @return An integer representing the scale used by the settlement engine.
   */
  @JsonProperty("scale")
  int settlementEngineScale();

  /**
   * The amount of units that the  settlement engine commits to settle (note that this value may be less-than or equal
   * to the originally requested amount in {@link InitiateSettlementRequest#requestedSettlementAmount()}, but the amount will
   * never be greater).
   *
   * @return A {@link BigInteger} representing the amount of units that the Settlement Engine commits to eventually
   * settle.
   */
  @JsonProperty("amount")
  BigInteger committedSettlementAmount();

}
