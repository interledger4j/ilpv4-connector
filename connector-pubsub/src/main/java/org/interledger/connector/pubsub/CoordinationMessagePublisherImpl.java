package org.interledger.connector.pubsub;

import static org.interledger.connector.pubsub.RedisPubSubConfig.PUBSUB_REDIS_TEMPLATE_BEAN_NAME;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

public class CoordinationMessagePublisherImpl implements CoordinationMessagePublisher {

  @Autowired
  private RedisTemplate<String, ?> pubsubRedisTemplate;
  @Autowired
  private ChannelTopic topic;

  public CoordinationMessagePublisherImpl(
    @Qualifier(PUBSUB_REDIS_TEMPLATE_BEAN_NAME) RedisTemplate<String, ?> pubsubRedisTemplate,
    ChannelTopic topic
  ) {
    this.pubsubRedisTemplate = pubsubRedisTemplate;
    this.topic = topic;
  }

  public void publish(String message) {
    pubsubRedisTemplate.convertAndSend(topic.getTopic(), message);
  }
}
