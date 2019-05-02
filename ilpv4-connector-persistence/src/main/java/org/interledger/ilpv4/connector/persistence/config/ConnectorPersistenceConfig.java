package org.interledger.ilpv4.connector.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The Connector's Spring Data configuration.
 */
@Configuration
@ComponentScan("org.interledger.ilpv4.connector.persistence")
@EnableJpaRepositories(basePackages = "org.interledger.ilpv4.connector.persistence.repositories")
@EntityScan("org.interledger.ilpv4.connector.persistence.entities")
@Import({
          H2ConnectorPersistenceConfig.class,
          RedisConnectorPersistenceConfig.class
        })

public class ConnectorPersistenceConfig {

  //  @Bean
  //  ObjectMapper objectMapper() {
  //    final ObjectMapper objectMapper = new ObjectMapper();
  //
  //    objectMapper.registerModule(new Jdk8Module());
  //
  //    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  //    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  //    objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
  //    objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
  //    objectMapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
  //    objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
  //
  //    return objectMapper;
  //  }
}
