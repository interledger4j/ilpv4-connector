package org.interledger.connector.transactions;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

public interface GeneratedFulfillmentPublisher {

  void publish(FulfillmentGeneratedEvent event);

}
