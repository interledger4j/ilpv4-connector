package org.interledger.ilpv4.connector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.ilpv4.connector.core.ConfigConstants.DOT;
import static org.interledger.ilpv4.connector.core.ConfigConstants.ILPV4__CONNECTOR;

/**
 * <p>Configuration for{@link SettlementEngineClient}.</p>
 */
@Configuration
public class SettlementEngineClientConfig {

  public static final String SETTLEMENT_ENGINE_CLIENT = "SETTLEMENT_ENGINE_CLIENT";

  private static final String ILPV4__CONNECTOR__SE = ILPV4__CONNECTOR + DOT + "settlementEngines";
  private static final String ILPV4__CONNECTOR__SE__CONN_DEFAULTS
    = ILPV4__CONNECTOR__SE + DOT + "connectionDefaults";

  private static final String ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__CONNECT_TIMEOUT_MILLIS =
    ILPV4__CONNECTOR__SE__CONN_DEFAULTS + DOT + "connectTimeoutMillis";
  private static final String ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__READ_TIMEOUT_MILLIS =
    ILPV4__CONNECTOR__SE__CONN_DEFAULTS + DOT + "readTimeoutMillis";
  private static final String ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__WRITE_TIMEOUT_MILLIS =
    ILPV4__CONNECTOR__SE__CONN_DEFAULTS + DOT + "writeTimeoutMillis";
  private static final String ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__MAX_IDLE_CONNECTIONS =
    ILPV4__CONNECTOR__SE__CONN_DEFAULTS + DOT + "maxIdleConnections";
  private static final String ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__KEEP_ALIVE_MINUTES =
    ILPV4__CONNECTOR__SE__CONN_DEFAULTS + DOT + "keepAliveMinutes";

  @Autowired
  Environment environment;

  /**
   * Applied when connecting a TCP socket to the target host. A value of 0 means no timeout, otherwise values must be
   * between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to 10.
   */
  @Value("${" + ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__CONNECT_TIMEOUT_MILLIS + ":1000}")
  private long defaultConnectTimeoutMillis;

  /**
   * Applied to both the TCP socket and for individual read IO operations. A value of 0 means no timeout, otherwise
   * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to
   * 10.
   */
  @Value("${" + ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__READ_TIMEOUT_MILLIS + ":1000}")
  private long defaultReadTimeoutMillis;

  /**
   * Applied to individual write IO operations. A value of 0 means no timeout, otherwise values must be between 1 and
   * {@link Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to 10.
   */
  @Value("${" + ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__WRITE_TIMEOUT_MILLIS + ":1000}")
  private long defaultWriteTimeoutMillis;

  @Value("${" + ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__MAX_IDLE_CONNECTIONS + ":5}")
  private int defaultMaxIdleConnections;

  @Value("${" + ILPV4__CONNECTOR__SE__CONNECTION_DEFAULTS__KEEP_ALIVE_MINUTES + ":1}")
  private long defaultConnectionKeepAliveMinutes;

  @Bean
  @Qualifier(SETTLEMENT_ENGINE_CLIENT)
  public ConnectionPool connectionPool() {
    return new ConnectionPool(defaultMaxIdleConnections, defaultConnectionKeepAliveMinutes, TimeUnit.MINUTES);
  }

  @Bean
  @Qualifier(SETTLEMENT_ENGINE_CLIENT)
  OkHttp3ClientHttpRequestFactory okHttp3ClientHttpRequestFactory(final ConnectionPool settlementEngineConnectionPool) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(defaultConnectTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.readTimeout(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.writeTimeout(defaultWriteTimeoutMillis, TimeUnit.MILLISECONDS);

    OkHttpClient client = builder.connectionPool(settlementEngineConnectionPool).build();
    return new OkHttp3ClientHttpRequestFactory(client);
  }

  @Bean
  @Qualifier(SETTLEMENT_ENGINE_CLIENT)
  RestTemplate restTemplate(
    ObjectMapper objectMapper, OkHttp3ClientHttpRequestFactory okHttp3ClientHttpRequestFactory
  ) {
    MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter(objectMapper);
    RestTemplate restTemplate = new RestTemplate(okHttp3ClientHttpRequestFactory);
    restTemplate.setMessageConverters(Lists.newArrayList(httpMessageConverter));
    return restTemplate;
  }


  // TODO: Add security. See BlastConfig for one example.

  @PostConstruct
  public void startup() {
  }
}
