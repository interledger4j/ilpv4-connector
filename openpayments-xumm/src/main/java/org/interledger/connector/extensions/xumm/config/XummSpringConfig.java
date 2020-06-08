package org.interledger.connector.extensions.xumm.config;


import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.OPEN_PAYMENTS_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;

import org.interledger.connector.payid.PayIdClient;
import org.interledger.connector.xumm.client.XummClient;
import org.interledger.connector.xumm.service.XummPaymentService;
import org.interledger.connector.xumm.service.XummUserTokenService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import okhttp3.HttpUrl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = OPEN_PAYMENTS_ENABLED, havingValue = TRUE)
public class XummSpringConfig {

  // FIXME get from config
  public static final String XUMM_API_KEY = "e28cb433-2886-4794-9e64-1d8b7f7783e2";
  public static final String XUMM_API_SECRET = "7d98c46a-d21f-4ee3-ad07-b48a674375cb";

  @Bean
  public XummClient xummClient(ObjectMapper objectMapper) {
    return XummClient.construct(objectMapper,
      HttpUrl.parse("https://xumm.app"),
      XUMM_API_KEY,
      XUMM_API_SECRET);
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
