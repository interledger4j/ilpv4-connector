package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;
import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import java.math.BigDecimal;
import java.time.Instant;

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
    try {
      GcpFulfillmentEvent gcpFulfillmentEvent = map(event);
      String payload = mapper.writeValueAsString(gcpFulfillmentEvent);
      template.publish(topicName, payload);
      try {
        Thread.sleep(1000); // FIXME
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } catch (JsonProcessingException e) {
      logger.warn("Could not serialize event ", e);
    }
  }


  private GcpFulfillmentEvent map(PacketFulfillmentEvent event) {
    return GcpFulfillmentEvent.builder()
      .destinationIlpAddress(event.outgoingPreparePacket().getDestination())
      .connectorIlpAddress(connectorAddress)
      .prevHopAccount(event.accountSettings().get().accountId())
      .nextHopAccount(event.destinationAccount().accountId())
      .prevHopAssetCode(event.accountSettings().get().assetCode())
      .nextHopAssetCode(event.destinationAccount().assetCode())
      .prevHopAmount(event.incomingPreparePacket().getAmount())
      .nextHopAmount(event.outgoingPreparePacket().getAmount())
      .prevHopAssetScale(event.accountSettings().get().assetScale())
      .nextHopAssetScale(event.destinationAccount().assetScale())
      .exchangeRate(BigDecimal.ONE) // FIXME how to fx?
      .timestamp(Instant.now())
      .fulfillment(event.fulfillment().getCondition())
      .build();
  }

}
