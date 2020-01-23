package org.interledger.connector.server.spring.settings.web;

import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

  public static final String PROBLEM = "PROBLEM";

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return ObjectMapperFactory.create();
  }

  @Bean
  @Qualifier(PROBLEM)
  public ObjectMapper problemsObjectMapper() {
    return ObjectMapperFactory.createObjectMapperForProblemsJson();
  }
}
