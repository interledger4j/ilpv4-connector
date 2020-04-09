package org.interledger.connector.payments;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

/**
 * Implementation of {@link FulfillmentGeneratedEventAggregator} that aggregates
 * {@link org.interledger.connector.events.FulfillmentGeneratedEvent} synchronously to the
 * provided {@link StreamPaymentManager} (i.e. no queues). Calls to {@code aggregate()} will block
 * until the transaction is created or updated in the persistence store.
 */
public class SynchronousFulfillmentGeneratedEventAggregator implements FulfillmentGeneratedEventAggregator {

  private final StreamPaymentManager streamPaymentManager;
  private final FulfillmentGeneratedEventConverter converter;

  public SynchronousFulfillmentGeneratedEventAggregator(StreamPaymentManager streamPaymentManager,
                                                        FulfillmentGeneratedEventConverter converter) {
    this.streamPaymentManager = streamPaymentManager;
    this.converter = converter;
  }

  @Override
  public void aggregate(FulfillmentGeneratedEvent event) {
    streamPaymentManager.merge(converter.convert(event));
  }

}
