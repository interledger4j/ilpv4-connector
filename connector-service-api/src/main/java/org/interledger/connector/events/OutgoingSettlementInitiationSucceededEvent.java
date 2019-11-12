package org.interledger.connector.events;

import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.core.Immutable;

/**
 * Indicates this Connector initiated a settlement with the settlement engine.
 */
@Immutable
public interface OutgoingSettlementInitiationSucceededEvent extends ConnectorEvent {

  static OutgoingSettlementInitiationSucceededEventBuilder builder() {
    return new OutgoingSettlementInitiationSucceededEventBuilder();
  }

  /**
   * The idempotency key used for retrying requests. Defined via IL-RFC-38.
   *
   * @return A {@link String}.
   */
  String idempotencyKey();

  /**
   * The amount that the Connector proposed to be settled, in Clearing units.
   *
   * @return A {@link SettlementQuantity}.
   */
  SettlementQuantity settlementQuantityInClearingUnits();

  /**
   * The amount that the Settlement Engine indicated it would settle, in Clearing units.
   *
   * @return A {@link SettlementQuantity}.
   */
  SettlementQuantity processedQuantityInClearingUnits();

  @Override
  default String message() {
    return "Settlement initiation succeeded";
  }

}
