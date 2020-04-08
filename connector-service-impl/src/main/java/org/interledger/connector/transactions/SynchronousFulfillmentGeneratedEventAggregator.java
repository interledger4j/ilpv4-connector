package org.interledger.connector.transactions;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

/**
 * Implementation of {@link FulfillmentGeneratedEventAggregator} that aggregates
 * {@link org.interledger.connector.events.FulfillmentGeneratedEvent} synchronously to the
 * provided {@link PaymentTransactionManager} (i.e. no queues). Calls to {@code aggregate()} will block
 * until the transaction is created or updated in the persistence store.
 */
public class SynchronousFulfillmentGeneratedEventAggregator implements FulfillmentGeneratedEventAggregator {

  private final PaymentTransactionManager paymentTransactionManager;
  private final FulfillmentGeneratedEventConverter converter;

  public SynchronousFulfillmentGeneratedEventAggregator(PaymentTransactionManager paymentTransactionManager,
                                                        FulfillmentGeneratedEventConverter converter) {
    this.paymentTransactionManager = paymentTransactionManager;
    this.converter = converter;
  }

  @Override
  public void aggregate(FulfillmentGeneratedEvent event) {
    paymentTransactionManager.merge(converter.convert(event));
  }

}
