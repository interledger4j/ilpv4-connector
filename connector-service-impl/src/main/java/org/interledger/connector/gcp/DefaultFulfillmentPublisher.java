package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultFulfillmentPublisher implements FulfillmentPublisher {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void publish(PacketFulfillmentEvent event) {
    logger.debug("Received event");
  }
}
