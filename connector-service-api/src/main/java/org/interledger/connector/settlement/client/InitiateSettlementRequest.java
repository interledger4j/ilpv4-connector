package org.interledger.connector.settlement.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.connector.settlement.client.ImmutableInitiateSettlementRequest;

import java.math.BigInteger;

/**
 * An object for modeling a request to the `/settlements` endpoint of a Settlement Engine.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableInitiateSettlementRequest.class)
@JsonDeserialize(as = ImmutableInitiateSettlementRequest.class)
public interface InitiateSettlementRequest {

  static ImmutableInitiateSettlementRequest.Builder builder() {
    return ImmutableInitiateSettlementRequest.builder();
  }

  /**
   * The connector's account scale.
   *
   * @return An integer representing the scale used by the settlement engine.
   */
  @JsonProperty("scale")
  int connectorAccountScale();

  /**
   * The amount of units that the settlement engine should attempt to settle on an underlying ledger.
   *
   * @return A {@link BigInteger}.
   */
  @JsonProperty("amount")
  BigInteger requestedSettlementAmount();
}
