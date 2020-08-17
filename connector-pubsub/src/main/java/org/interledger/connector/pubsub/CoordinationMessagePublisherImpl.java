package org.interledger.connector.pubsub;

import static org.interledger.connector.pubsub.RedisPubSubConfig.PUBSUB_REDIS_TEMPLATE_BEAN_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.Objects;
import java.util.UUID;

public class CoordinationMessagePublisherImpl implements CoordinationMessagePublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationMessagePublisherImpl.class);
  private final RedisTemplate<String, ?> pubsubRedisTemplate;
  private final ChannelTopic topic;
  private final ObjectMapper objectMapper;
  private final CoordinationMessageIdGenerator coordinatedMessageIdGenerator;
  private final UUID applicationCoordinationUuid;

  public CoordinationMessagePublisherImpl(
    @Qualifier(PUBSUB_REDIS_TEMPLATE_BEAN_NAME) RedisTemplate<String, ?> pubsubRedisTemplate,
    ChannelTopic topic,
    ObjectMapper objectMapper,
    UUID applicationCoordinationUuid,
    CoordinationMessageIdGenerator coordinatedMessageIdGenerator
  ) {
    this.pubsubRedisTemplate = pubsubRedisTemplate;
    this.topic = topic;
    this.objectMapper = objectMapper;
    this.applicationCoordinationUuid = applicationCoordinationUuid;
    this.coordinatedMessageIdGenerator = coordinatedMessageIdGenerator;
  }

  public void publish(AbstractCoordinatedEvent message) {
    Objects.requireNonNull(message);

    if (message.receivedViaCoordination()) {
      throw new IllegalArgumentException("Cannot republish a message received via coordination already");
    }

    try {
      CoordinationMessage coordinationMessage = CoordinationMessage.builder()
        .applicationCoordinationUuid(applicationCoordinationUuid)
        .messageUuid(coordinatedMessageIdGenerator.generatedMessageUUID())
        .messageClassName(message.getClass().getName())
        .contents(objectMapper.writeValueAsBytes(message))
        .build();
      String serialized = objectMapper.writeValueAsString(coordinationMessage);
      pubsubRedisTemplate.convertAndSend(topic.getTopic(), serialized);
    } catch (Exception e) {
      LOGGER.error("Cannot serialize entity to publish to Redis via Jackson={} exception={}", message, e.getMessage());
    }

  }
}
