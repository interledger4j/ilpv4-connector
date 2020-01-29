package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFullfillmentEvent;
import org.interledger.connector.events.PacketRejectionEvent;

/**
 * Publisher for {@link PacketFullfillmentEvent} and {@link PacketRejectionEvent} to GCP pubsub
 */
public interface GcpPacketResponseEventPublisher {

  /**
   * publishes the fulfillment event to GCP pubsub
   * @param event
   */
  void publish(PacketFullfillmentEvent event);

  /**
   * publishes the rejection event to GCP pubsub
   * @param event
   */
  void publish(PacketRejectionEvent event);

}
