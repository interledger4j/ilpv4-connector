package org.interledger.connector.payments;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

/**
 * Aggregates {@link FulfillmentGeneratedEvent} into {@link StreamPayment}s.
 * The first time a new stream payment is seen for a business key,
 * it is created/stored in the underlying persistence store. Subsequent events that map to an existing stream payment
 * are aggregated into the same stream payment, such that the numeric values are summed and any non-numeric values are
 * overwritten with the latest value.
 *
 * Implementations may choose whether to aggregate in-real time or use eventual consistency.
 */
public interface FulfillmentGeneratedEventAggregator {

  /**
   * Accepts a {@link FulfillmentGeneratedEvent} to be eventually aggregated it into a new or existing stream payment
   * by accountId and streamPaymentId. This call may or may not block depending on implementation.
   * @param event
   */
  void aggregate(FulfillmentGeneratedEvent event);

}
