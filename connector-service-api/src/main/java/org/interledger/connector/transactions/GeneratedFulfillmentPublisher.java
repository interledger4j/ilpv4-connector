package org.interledger.connector.transactions;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

import com.google.common.eventbus.Subscribe;

public interface GeneratedFulfillmentPublisher {

  @Subscribe
  void receive(FulfillmentGeneratedEvent event);

}
