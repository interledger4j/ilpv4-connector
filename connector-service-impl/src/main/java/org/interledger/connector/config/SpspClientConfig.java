package org.interledger.connector.config;

import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpspClientConfig {

  @Bean
  protected SpspClient spspClient() {
    return new SimpleSpspClient();
  }

}
