package com.sappenin.interledger.ilpv4.connector.server.spring.settings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    return ObjectMapperFactory.create();
  }
}
