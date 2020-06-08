package org.interledger.connector.extensions.xumm.config;


import org.interledger.connector.payid.PayIdClient;
import org.interledger.connector.xumm.client.XummClient;
import org.interledger.connector.xumm.service.XummPaymentService;
import org.interledger.connector.xumm.service.XummUserTokenService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("interledger.connector.xumm.apiKey")
public class XummSpringConfig {

  @Value("${interledger.connector.xumm.apiKey}")
  private String xummApiKey;

  @Value("${interledger.connector.xumm.apiSecret}")
  private String xummApiSecret;

  @Bean
  public XummClient xummClient(
    ObjectMapper objectMapper) {
    return XummClient.construct(objectMapper,
      HttpUrl.parse("https://xumm.app"),
      xummApiKey,
      xummApiSecret);
  }

  @Bean
  public XummUserTokenService xummUserTokenService() {
    return new XummUserTokenService();
  }


  @Bean
  public XummPaymentService xummPaymentService(
    XummClient xummClient,
    XummUserTokenService xummUserTokenService,
    EventBus eventBus,
    ObjectMapper objectMapper,
    PayIdClient payIdClient) {
    return new XummPaymentService(xummClient, payIdClient, xummUserTokenService, eventBus, objectMapper);
  }

}
