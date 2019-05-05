package org.interledger.ilpv4.connector.persistence.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@ConditionalOnProperty(name = "spring.data.redis.repositories.enabled", havingValue = "true")
@EnableRedisRepositories(basePackages = "org.interledger.ilpv4.connector.persistence.repositories")
public class RedisConnectorPersistenceConfig {

  // TODO: Require JacksonConfig

  @Bean
  JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    JedisConnectionFactory jedisConFactory = new JedisConnectionFactory(config);
    return jedisConFactory;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(jedisConnectionFactory());
    return template;
  }

}
