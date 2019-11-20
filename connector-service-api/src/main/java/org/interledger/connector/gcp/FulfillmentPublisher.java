package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;

/**
 * Publisher {@link PacketFulfillmentEvent}
 */
public interface FulfillmentPublisher {

  /**
   * publishes the event
   * @param event
   */
  void publish(PacketFulfillmentEvent event);

}
