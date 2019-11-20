package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;
import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import java.time.Clock;
import java.time.Instant;
import javax.annotation.PreDestroy;

/**
 * Default implementation of {@link FulfillmentPublisher} that publishes {@link PacketFulfillmentEvent}s to the
 * event bus
 */
public class DefaultFulfillmentPublisher implements FulfillmentPublisher {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PubSubTemplate template;
  private final String topicName;
  private final InterledgerAddress connectorAddress;
  private final ObjectMapper mapper;
  private final Clock clock;

  public DefaultFulfillmentPublisher(PubSubTemplate template,
                                     String topicName,
                                     InterledgerAddress connectorAddress,
                                     ObjectMapper mapper,
                                     Clock clock) {
    this.template = template;
    this.topicName = topicName;
    this.connectorAddress = connectorAddress;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  public void publish(PacketFulfillmentEvent event) {
    logger.debug("Received event");
    try {
      GcpFulfillmentEvent gcpFulfillmentEvent = map(event);
      String payload = mapper.writeValueAsString(gcpFulfillmentEvent);
      template.publish(topicName, payload);
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
      .exchangeRate(event.exchangeRate())
      .timestamp(Instant.now(clock))
      .fulfillment(event.fulfillment().getCondition())
      .build();
  }

}
