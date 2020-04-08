package org.interledger.connector.transactions;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

/**
 * Aggregates {@link FulfillmentGeneratedEvent} into {@link Transaction}s.
 * The first time a new transaction is seen for a business key,
 * it is created/stored in the underlying persistence store. Subsequent events that map to an existing transaction
 * are aggregated into the same transaction, such that the numeric values are summed and any non-numeric values are
 * overwritten with the latest value.
 *
 * Implementations may choose whether to aggregate in-real time or use eventual consistency.
 */
public interface FulfillmentGeneratedEventAggregator {

  /**
   * Accepts a {@link FulfillmentGeneratedEvent} to be eventually aggregated it into a new or existing transaction
   * by accountId and transactionId. This call may or may not block depending on implementation.
   * @param event
   */
  void aggregate(FulfillmentGeneratedEvent event);

}
