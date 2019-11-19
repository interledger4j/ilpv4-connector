package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;
import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

public class DefaultFulfillmentPublisher implements FulfillmentPublisher {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PubSubTemplate template;
  private final String topicName;
  private final InterledgerAddress connectorAddress;
  private final ObjectMapper mapper;

  public DefaultFulfillmentPublisher(PubSubTemplate template, String topicName, InterledgerAddress connectorAddress,
                                     ObjectMapper mapper) {
    this.template = template;
    this.topicName = topicName;
    this.connectorAddress = connectorAddress;
    this.mapper = mapper;
  }


  @Override
  public void publish(PacketFulfillmentEvent event) {
    logger.debug("Received event");

    GcpFulfillmentEvent gcpFulfillmentEvent = null;

    try {
      template.publish(topicName, mapper.writeValueAsString(gcpFulfillmentEvent)); // FIXME send real event
    } catch (JsonProcessingException e) {
      logger.warn("Could not serialize event " + gcpFulfillmentEvent, e);
    }
  }


  private GcpFulfillmentEvent map(PacketFulfillmentEvent event) {
    return GcpFulfillmentEvent.builder()
      .destinationIlpAddress(event.preparePacket().getDestination())
      .connectorIlpAddress(connectorAddress)
      .prevHopAccount(event.accountSettings().get().accountId())
      .nextHopAccount(event.destinationAccount().accountId())
      .fulfillment(event.responsePacket().getFulfillment().getCondition())
      .build();
  }

}
