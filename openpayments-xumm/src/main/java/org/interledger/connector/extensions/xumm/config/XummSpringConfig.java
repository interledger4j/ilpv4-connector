package org.interledger.connector.extensions.xumm.config;


import org.interledger.connector.xumm.client.XummClient;
import org.interledger.connector.xumm.service.XummPaymentService;
import org.interledger.connector.xumm.service.XummUserTokenService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XummSpringConfig {


  @Bean
  public XummClient xummClient(ObjectMapper objectMapper) {
    return XummClient.construct(objectMapper, "e28cb433-2886-4794-9e64-1d8b7f7783e2", "7d98c46a-d21f-4ee3-ad07-b48a674375cb");
  }

  @Bean
  public XummUserTokenService xummUserTokenService() {
    return new XummUserTokenService();
  }


  @Bean
  public XummPaymentService xummPaymentService(
    XummClient xummClient, XummUserTokenService xummUserTokenService, EventBus eventBus, ObjectMapper objectMapper) {
    return new XummPaymentService(xummClient, xummUserTokenService, eventBus, objectMapper);
  }

}
