package org.interledger.connector.events;

import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.settlement.SettlementServiceException;
import org.interledger.core.Immutable;

/**
 * Indicates this Connector initiated a settlement with the settlement engine.
 */
@Immutable
public interface OutgoingSettlementInitiationFailedEvent extends ConnectorEvent {

  static OutgoingSettlementInitiationFailedEventBuilder builder() {
    return new OutgoingSettlementInitiationFailedEventBuilder();
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
   * The {@link SettlementServiceException} that triggered the settlement failure.
   *
   * @return A {@link SettlementServiceException}.
   */
  SettlementServiceException settlementServiceException();

  @Override
  default String message() {
    return "Settlement initiation failed";
  }

}
