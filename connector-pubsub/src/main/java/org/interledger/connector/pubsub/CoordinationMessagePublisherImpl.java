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

  private RedisTemplate<String, ?> pubsubRedisTemplate;
  private ChannelTopic topic;
  private ObjectMapper objectMapper;
  private CoordinationDedupeCache coordinationDedupeCache;

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationMessagePublisherImpl.class);

  public CoordinationMessagePublisherImpl(
    @Qualifier(PUBSUB_REDIS_TEMPLATE_BEAN_NAME) RedisTemplate<String, ?> pubsubRedisTemplate,
    ChannelTopic topic,
    ObjectMapper objectMapper,
    CoordinationDedupeCache coordinationDedupeCache
  ) {
    this.pubsubRedisTemplate = pubsubRedisTemplate;
    this.topic = topic;
    this.objectMapper = objectMapper;
    this.coordinationDedupeCache = coordinationDedupeCache;
  }

  public void publish(Object message) {
    try {
      Objects.requireNonNull(message);
      UUID messageUuid = UUID.randomUUID();
      CoordinationMessage coordinationMessage = CoordinationMessage.builder()
        .messageUuid(messageUuid)
        .messageType(message.getClass())
        .contents(objectMapper.writeValueAsBytes(message))
        .build();
      String serialized = objectMapper.writeValueAsString(coordinationMessage);
      coordinationDedupeCache.record(messageUuid);
      pubsubRedisTemplate.convertAndSend(topic.getTopic(), serialized);
    } catch (Exception e) {
      LOGGER.error("Cannot serialize entity to publish to Redis via Jackson: {}", message);
    }

  }
}
