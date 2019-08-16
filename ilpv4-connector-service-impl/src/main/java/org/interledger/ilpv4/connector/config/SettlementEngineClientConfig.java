package org.interledger.ilpv4.connector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import org.interledger.ilpv4.connector.settlement.client.OkHttpSettlementEngineClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static okhttp3.CookieJar.NO_COOKIES;

/**
 * <p>Configuration for{@link SettlementEngineClient}.</p>
 */
@Configuration
public class SettlementEngineClientConfig {

  public static final String SETTLEMENT_ENGINE_CLIENT = "SETTLEMENT_ENGINE_CLIENT";

  @Bean
  @Qualifier(SETTLEMENT_ENGINE_CLIENT)
  protected ConnectionPool seConnectionPool(
    @Value("${ilpv4.connector.settlementEngines.connectionDefaults.maxIdleConnections:5}")
    final int defaultMaxIdleConnections,
    @Value("${ilpv4.connector.settlementEngines.connectionDefaults.keepAliveMinutes:5}")
    final long defaultConnectionKeepAliveMinutes
  ) {
    return new ConnectionPool(defaultMaxIdleConnections, defaultConnectionKeepAliveMinutes, TimeUnit.MINUTES);
  }

  /**
   * @param settlementEngineConnectionPool A {@link ConnectionPool} as configured above.
   * @param defaultConnectTimeoutMillis    Applied when connecting a TCP socket to the target host. A value of 0 means
   *                                       no timeout, otherwise values must be between 1 and {@link Integer#MAX_VALUE}
   *                                       when converted to milliseconds. If unspecified, defaults to 10000.
   * @param defaultReadTimeoutMillis       Applied to both the TCP socket and for individual read IO operations. A value
   *                                       of 0 means no timeout, otherwise values must be between 1 and {@link
   *                                       Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults
   *                                       to 30000.
   * @param defaultWriteTimeoutMillis      Applied to individual write IO operations. A value of 0 means no timeout,
   *                                       otherwise values must be between 1 and {@link Integer#MAX_VALUE} when
   *                                       converted to milliseconds. If unspecified, defaults to 30000.
   *
   * @return A {@link OkHttp3ClientHttpRequestFactory}.
   */
  @Bean
  @Qualifier(SETTLEMENT_ENGINE_CLIENT)
  protected OkHttpClient okHttpClient(
    @Qualifier(SETTLEMENT_ENGINE_CLIENT) final ConnectionPool settlementEngineConnectionPool,
    @Value("${ilpv4.connector.settlementEngines.connectionDefaults.connectTimeoutMillis:10000}")
    final long defaultConnectTimeoutMillis,
    @Value("${ilpv4.connector.settlementEngines.connectionDefaults.readTimeoutMillis:30000}")
    final long defaultReadTimeoutMillis,
    @Value("${ilpv4.connector.settlementEngines.connectionDefaults.writeTimeoutMillis:30000}")
    final long defaultWriteTimeoutMillis
  ) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(defaultConnectTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.readTimeout(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.writeTimeout(defaultWriteTimeoutMillis, TimeUnit.MILLISECONDS);

    return builder.connectionPool(settlementEngineConnectionPool).build();
  }

  @Bean
  protected SettlementEngineClient settlementEngineClient(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
    return new OkHttpSettlementEngineClient(okHttpClient, objectMapper);
  }

  // TODO: Add security. See BlastConfig for one example.
}
