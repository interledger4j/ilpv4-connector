package com.sappenin.interledger.ilpv4.connector.events;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.Immutable;
import org.interledger.ilpv4.connector.core.events.ConnectorEvent;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;

/**
 * Indicates this Connector initated a settlement with the settlement engine.
 */
@Immutable
public interface SettlementInitiatedEvent extends ConnectorEvent<SettlementQuantity> {

  static SettlementInitiatedEventBuilder builder() {
    return new SettlementInitiatedEventBuilder();
  }

  String idempotencyKey();

  AccountSettings accountSettings();

  SettlementQuantity settlementQuantityInClearingUnits();

  SettlementQuantity processedQuantity();

  default SettlementQuantity object() {
    return this.processedQuantity();
  }

  @Override
  default String message() {
    return String.format("Local Settlement Processed: " +
        "idempotencyKey=%s " +
        "accountSettings=%s " +
        "settlementQuantityInClearingUnits=%s " +
        "processedQuantity=%s",
      idempotencyKey(), accountSettings(), settlementQuantityInClearingUnits(), processedQuantity()
    );
  }

}
