package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.UUID;

@Configuration
public class RedisPubSubConfig {

  public static final String PUBSUB_REDIS_TEMPLATE_BEAN_NAME = "pubsubRedisTemplate";

  public static final String COORDINATION_TOPIC_NAME = "connector-coordination-topic";

  @Autowired
  protected LettuceConnectionFactory lettuceConnectionFactory;

  @Autowired
  protected EventBus eventBus;

  @Autowired
  protected ObjectMapper objectMapper;

  private static final UUID APPLICATION_COORDINATION_UUID = UUID.randomUUID();

  @Bean
  UUID applicationCoordinationUuid() {
    return APPLICATION_COORDINATION_UUID;
  }

  @Bean
  MessageListenerAdapter coordinationMessageListener() {
    return new MessageListenerAdapter(new CoordinationMessageSubscriber(objectMapper, eventBus,
      applicationCoordinationUuid(), coordinatedProxyGenerator()));
  }

  @Bean
  RedisMessageListenerContainer coordinationMessageListenerContainer() {
    RedisMessageListenerContainer container
      = new RedisMessageListenerContainer();
    container.setConnectionFactory(lettuceConnectionFactory);
    container.addMessageListener(coordinationMessageListener(), coordinationTopic());
    return container;
  }

  @Bean
  ChannelTopic coordinationTopic() {
    return new ChannelTopic(COORDINATION_TOPIC_NAME);
  }

  @Bean
  CoordinationMessagePublisher coordinationRedisPublisher() {
    return new CoordinationMessagePublisherImpl(pubsubRedisTemplate(), coordinationTopic(), objectMapper,
      applicationCoordinationUuid(), coordinatedMessageIdGenerator());
  }

  @Bean(PUBSUB_REDIS_TEMPLATE_BEAN_NAME)
  public RedisTemplate<String, ?> pubsubRedisTemplate() {
    final RedisTemplate<String, ?> template = new RedisTemplate<>();

    template.setEnableDefaultSerializer(true);
    template.setDefaultSerializer(new StringRedisSerializer());
    template.setConnectionFactory(lettuceConnectionFactory);

    return template;
  }

  @Bean
  CoordinationEventBusBridge coordinationEventBusBridge() {
    return new CoordinationEventBusBridge(coordinationRedisPublisher(), eventBus);
  }

  @Bean
  CoordinationMessageIdGenerator coordinatedMessageIdGenerator() {
    return new CoordinationMessageIdGeneratorImpl();
  }

  @Bean
  CoordinationProxyGenerator coordinatedProxyGenerator() {
    return new CoordinationProxyGeneratorImpl();
  }

}
