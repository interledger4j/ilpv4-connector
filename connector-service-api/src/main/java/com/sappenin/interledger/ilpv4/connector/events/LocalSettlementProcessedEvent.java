package com.sappenin.interledger.ilpv4.connector.events;

import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.core.Immutable;
import org.interledger.ilpv4.connector.core.events.ConnectorEvent;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;

/**
 * Indicates a local settlement was processed by this Connector's Settlement Engine and propagated to this Connector via
 * the `/accounts/{accountId}/settlements` endpoint.
 */
@Immutable
public interface LocalSettlementProcessedEvent extends ConnectorEvent<SettlementQuantity> {

  static LocalSettlementProcessedEventBuilder builder() {
    return new LocalSettlementProcessedEventBuilder();
  }

  String idempotencyKey();

  SettlementEngineAccountId settlementEngineAccountId();

  SettlementQuantity incomingSettlementInSettlementUnits();

  SettlementQuantity processedQuantity();

  default SettlementQuantity object() {
    return this.processedQuantity();
  }

  @Override
  default String message() {
    return String.format("Local Settlement Processed: " +
        "idempotencyKey=%s " +
        "settlementEngineAccountId=%s " +
        "incomingSettlementInSettlementUnits=%s " +
        "processedQuantity=%s",
      idempotencyKey(), settlementEngineAccountId(), incomingSettlementInSettlementUnits(), processedQuantity()
    );
  }

}
