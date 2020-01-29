package org.interledger.connector.pubsub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisPubSubConfig {

  public static final String PUBSUB_REDIS_TEMPLATE_BEAN_NAME = "pubsubRedisTemplate";

  public static final String COORDINATION_TOPIC_NAME = "connector-coordination-topic";

  @Autowired
  protected LettuceConnectionFactory lettuceConnectionFactory;

  @Bean
  MessageListenerAdapter coordinationMessageListener() {
    return new MessageListenerAdapter(new CoordinationMessageSubscriber());
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
    return new CoordinationMessagePublisherImpl(pubsubRedisTemplate(), coordinationTopic());
  }

  @Bean(PUBSUB_REDIS_TEMPLATE_BEAN_NAME)
  public RedisTemplate<String, ?> pubsubRedisTemplate() {
    final RedisTemplate<String, ?> template = new RedisTemplate<>();

    template.setEnableDefaultSerializer(true);
    template.setDefaultSerializer(new StringRedisSerializer());
    template.setConnectionFactory(lettuceConnectionFactory);

    return template;
  }
}
