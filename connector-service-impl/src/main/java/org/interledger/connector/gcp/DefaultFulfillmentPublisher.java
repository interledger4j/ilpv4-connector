package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

public class DefaultFulfillmentPublisher implements FulfillmentPublisher {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PubSubTemplate template;
  private final String topicName;

  public DefaultFulfillmentPublisher(PubSubTemplate template, String topicName) {
    this.template = template;
    this.topicName = topicName;
  }


  @Override
  public void publish(PacketFulfillmentEvent event) {
    logger.debug("Received event");
    template.publish(topicName, "{}"); // FIXME send real event
  }
}
