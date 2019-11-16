package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;

public interface FulfillmentPublisher {

  void publish(PacketFulfillmentEvent event);

}
