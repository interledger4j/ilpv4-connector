package org.interledger.ilpv4.connector.settlement.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.connector.accounts.SettlementEngineAccountId;

/**
 * Models a settlement account's state as reported by the settlement engine.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSettlementAccount.class)
@JsonDeserialize(as = ImmutableSettlementAccount.class)
public interface SettlementAccount {

  static ImmutableSettlementAccount.Builder builder() {
    return ImmutableSettlementAccount.builder();
  }

  /**
   * The connector's account scale.
   *
   * @return An integer representing the scale used by the settlement engine.
   */
  @JsonProperty("id")
  SettlementEngineAccountId settlementAccountId();
}
