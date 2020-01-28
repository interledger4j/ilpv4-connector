package org.interledger.connector.gcp;

import org.interledger.connector.events.PacketFullfillmentEvent;
import org.interledger.connector.events.PacketRejectionEvent;
import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import java.time.Clock;
import java.time.Instant;

/**
 * Default implementation of {@link GcpPacketResponseEventPublisher} that publishes {@link PacketFullfillmentEvent}
 * to the event bus
 */
public class DefaultGcpPacketResponseEventPublisher implements GcpPacketResponseEventPublisher {

  public static final String STATUS_FULFILLED = "FULFILLED";
  public static final String STATUS_REJECTED = "REJECTED";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PubSubTemplate template;
  private final String topicName;
  private final InterledgerAddress connectorAddress;
  private final ObjectMapper mapper;
  private final Clock clock;

  public DefaultGcpPacketResponseEventPublisher(PubSubTemplate template,
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
  public void publish(PacketFullfillmentEvent event) {
    logger.debug("Received event");
    try {
      GcpPacketResponseEvent gcpFulfillmentEvent = mapFulfillment(event);
      String payload = mapper.writerFor(GcpPacketResponseEvent.class).writeValueAsString(gcpFulfillmentEvent);
      template.publish(topicName, payload);
    } catch (JsonProcessingException e) {
      logger.warn("Could not serialize event ", e);
    }
  }

  @Override
  public void publish(PacketRejectionEvent event) {
    logger.debug("Received event");
    try {
      GcpPacketResponseEvent gcpFulfillmentEvent = mapRejection(event);
      String payload = mapper.writeValueAsString(gcpFulfillmentEvent);
      template.publish(topicName, payload);
    } catch (JsonProcessingException e) {
      logger.warn("Could not serialize event ", e);
    }
  }

  private GcpPacketResponseEvent mapFulfillment(PacketFullfillmentEvent event) {
    ImmutableGcpPacketResponseEvent.Builder builder = GcpPacketResponseEvent.builder()
      .connectorIlpAddress(connectorAddress)
      .prevHopAccount(event.accountSettings().get().accountId())
      .prevHopAssetCode(event.accountSettings().get().assetCode())
      .prevHopAmount(event.incomingPreparePacket().getAmount())
      .prevHopAssetScale(event.accountSettings().get().assetScale())
      .nextHopAccount(event.destinationAccount().accountId())
      .nextHopAssetCode(event.destinationAccount().assetCode())
      .nextHopAssetScale(event.destinationAccount().assetScale())
      .exchangeRate(event.exchangeRate())
      .fulfillment(event.fulfillment().getCondition())
      .destinationIlpAddress(event.incomingPreparePacket().getDestination())
      .timestamp(Instant.now(clock))
      .status(STATUS_FULFILLED);

    event.outgoingPreparePacket().map(outgoing -> builder.nextHopAmount(outgoing.getAmount()));

    return builder.build();
  }

  private GcpPacketResponseEvent mapRejection(PacketRejectionEvent event) {
    ImmutableGcpPacketResponseEvent.Builder builder = GcpPacketResponseEvent.builder()
      .connectorIlpAddress(connectorAddress)
      .prevHopAccount(event.accountSettings().get().accountId())
      .prevHopAssetCode(event.accountSettings().get().assetCode())
      .prevHopAmount(event.incomingPreparePacket().getAmount())
      .prevHopAssetScale(event.accountSettings().get().assetScale())
      .rejectionMessage(event.rejection().getMessage())
      .rejectionCode(event.rejection().getCode().getCode())
      .rejectionTriggeredBy(event.rejection().getTriggeredBy().map(InterledgerAddress::getValue).get())
      .destinationIlpAddress(event.incomingPreparePacket().getDestination())
      .timestamp(Instant.now(clock))
      .status(STATUS_REJECTED);

    event.outgoingPreparePacket().map(outgoing -> builder.nextHopAmount(outgoing.getAmount()));
    event.exchangeRate().map(builder::exchangeRate);

    event.destinationAccount().map(account -> builder.nextHopAccount(account.accountId())
      .nextHopAssetCode(account.assetCode())
      .nextHopAssetScale(account.assetScale())
    );
    return builder.build();
  }

}
