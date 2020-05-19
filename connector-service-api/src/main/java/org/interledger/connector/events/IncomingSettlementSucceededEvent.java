package org.interledger.connector.events;

import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.core.Immutable;

import java.util.Optional;

/**
 * Indicates a local settlement was processed by this Connector's Settlement Engine and propagated to this Connector via
 * the `/accounts/{accountId}/settlements` endpoint.
 */
@Immutable
public interface IncomingSettlementSucceededEvent extends ConnectorEvent {

  static IncomingSettlementSucceededEventBuilder builder() {
    return new IncomingSettlementSucceededEventBuilder();
  }

  /**
   * The idempotency key used for retrying requests. Defined via IL-RFC-38.
   *
   * @return A {@link String}.
   */
  String idempotencyKey();

  /**
   * The unique identifier of the settlement engine account that sent an incoming settlement to this Connector.
   *
   * @return A {@link SettlementEngineAccountId}.
   */
  SettlementEngineAccountId settlementEngineAccountId();

  /**
   * The number of units in the associated settlement request that were originally requested to be settled for,
   * denominated in the settlement engine's units.
   *
   * @return A {@link SettlementQuantity}.
   */
  SettlementQuantity incomingSettlementInSettlementUnits();

  /**
   * The number of units in the associated settlement request that were actually settled, denominated in the settlement
   * engine's units.
   *
   * @return A {@link SettlementQuantity}.
   */
  SettlementQuantity processedQuantity();

  @Override
  default Optional<String> message() {
    return Optional.of("Local settlement succeeded.");
  }

}
