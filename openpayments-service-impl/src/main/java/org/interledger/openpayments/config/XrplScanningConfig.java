package org.interledger.openpayments.config;

import org.interledger.openpayments.xrpl.XrplScanningService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;

@Configuration
public class XrplScanningConfig {

  public static final String XRPL_SCANNING = "XRPL_SCANNING";

  @Value("${interledger.connector.xrpl.websocketUrl:wss://s.altnet.rippletest.net:51233}")
  protected String xrplWebsocketUrl = "wss://s.altnet.rippletest.net:51233"; //testnet

  @Value("${interledger.connector.xrpl.socketTimeoutInMillis:5000}")
  protected String socketTimeoutInMillis;

  /**
   * Number of times we allow for detecting failures communicating with the websocket
   * before we attempt a new request.
   */
  @Value("${interledger.connector.xrpl.failureCountThreshold:25}")
  protected String failureCountThreshold;

  @Value("${interledger.connector.xrpl.enabled:false}")
  private boolean xrplScanningEnabled;


  @Autowired
  protected EventBus eventBus;

  @Autowired
  protected ObjectMapper objectMapper;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @PostConstruct
  private void init() {
    if (xrplScanningEnabled) {
      xrplScanningService().init();
    }
  }

  @Bean
  @Qualifier(XRPL_SCANNING)
  protected OkHttpClient xrplHttpClient() {
    int timeout = 5000;

    try {
      if (socketTimeoutInMillis != null) {
        timeout = Integer.parseInt(socketTimeoutInMillis);
      }
    } catch (Exception e) {
      logger.warn("Misconfigured XRPL scanner socket timeout", e);
    }

    OkHttpClient client = new OkHttpClient.Builder()
      .readTimeout(timeout, TimeUnit.MILLISECONDS)
      .build();
    return client;
  }

  @Bean
  @Qualifier(XRPL_SCANNING)
  protected Supplier<Request> xrplWebsocketRequestSupplier() {
    return () -> new Request.Builder().url(xrplWebsocketUrl).build();
  }

  @Bean
  protected XrplScanningService xrplScanningService() {
    int failureThreshold = 25;
    try {
      failureThreshold = Integer.parseInt(failureCountThreshold);
    } catch (Exception e) {
      logger.warn("Misconfigured XRPL failure threshold", e);
    }
    return new XrplScanningService(
      xrplHttpClient(),
      xrplWebsocketRequestSupplier(),
      eventBus,
      objectMapper,
      failureThreshold
    );
  }

}
