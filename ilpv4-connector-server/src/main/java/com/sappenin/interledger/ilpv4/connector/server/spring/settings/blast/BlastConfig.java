package com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.accounts.BlastAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters.OerPreparePacketHttpMessageConverter;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkFactory;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.crypto.Decryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.DEFAULT_JWT_TOKEN_ISSUER;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.ENABLED_PROTOCOLS;
import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.ilpv4.connector.core.ConfigConstants.DOT;
import static org.interledger.ilpv4.connector.core.ConfigConstants.ILPV4__CONNECTOR;
import static org.interledger.ilpv4.connector.core.ConfigConstants.TRUE;

/**
 * <p>Configures ILP-over-HTTP (i.e., BLAST), which provides a single Link-layer mechanism for this Connector's
 * peers.</p>
 *
 * <p>This type of Connector supports a single HTTP/2 Server endpoint in addition to multiple, statically configured,
 * BLAST client links.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = BLAST_ENABLED, havingValue = TRUE)
public class BlastConfig {

  public static final String BLAST = "blast";
  private static final String ILPV4__CONNECTOR__ILP_OVER_HTTP = ILPV4__CONNECTOR + DOT + "ilpOverHttp";
  private static final String ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS
    = ILPV4__CONNECTOR__ILP_OVER_HTTP + DOT + "connectionDefaults";
  private static final String ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__CONNECT_TIMEOUT_MILLIS =
    ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS + DOT + "connectTimeoutMillis";
  private static final String ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__READ_TIMEOUT_MILLIS =
    ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS + DOT + "readTimeoutMillis";
  private static final String ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__WRITE_TIMEOUT_MILLIS =
    ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS + DOT + "writeTimeoutMillis";
  private static final String ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__MAX_IDLE_CONNECTIONS =
    ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS + DOT + "maxIdleConnections";
  private static final String ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__KEEP_ALIVE_MINUTES =
    ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS + DOT + "keepAliveMinutes";

  /**
   * Applied when connecting a TCP socket to the target host. A value of 0 means no timeout, otherwise values must be
   * between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to 10.
   */
  @Value("${" + ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__CONNECT_TIMEOUT_MILLIS + ":1000}")
  long defaultConnectTimeoutMillis;

  /**
   * Applied to both the TCP socket and for individual read IO operations. A value of 0 means no timeout, otherwise
   * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to
   * 10.
   */
  @Value("${" + ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__READ_TIMEOUT_MILLIS + ":1000}")
  long defaultReadTimeoutMillis;

  /**
   * Applied to individual write IO operations. A value of 0 means no timeout, otherwise values must be between 1 and
   * {@link Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to 10.
   */
  @Value("${" + ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__WRITE_TIMEOUT_MILLIS + ":1000}")
  long defaultWriteTimeoutMillis;

  @Value("${" + ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__MAX_IDLE_CONNECTIONS + ":5}")
  int defaultMaxIdleConnections;

  @Value("${" + ILPV4__CONNECTOR__ILP_OVER_HTTP__CONNECTION_DEFAULTS__KEEP_ALIVE_MINUTES + ":5}")
  long defaultConnectionKeepAliveMinutes;

  @Autowired
  Environment environment;

  @Autowired
  LinkEventEmitter linkEventEmitter;

  @Autowired
  LinkFactoryProvider linkFactoryProvider;

  @Autowired
  @Qualifier(BLAST)
  RestTemplate blastRestTemplate;

  @Autowired
  Decryptor decryptor;

  @Bean
  @Qualifier(BLAST)
  public ConnectionPool connectionPool() {
    return new ConnectionPool(defaultMaxIdleConnections, defaultConnectionKeepAliveMinutes, TimeUnit.MINUTES);
  }

  @Bean
  @Qualifier(BLAST)
  OkHttp3ClientHttpRequestFactory okHttp3ClientHttpRequestFactory(final ConnectionPool ilpOverHttpConnectionPool) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(defaultConnectTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.readTimeout(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.writeTimeout(defaultWriteTimeoutMillis, TimeUnit.MILLISECONDS);

    OkHttpClient client = builder.connectionPool(ilpOverHttpConnectionPool).build();
    return new OkHttp3ClientHttpRequestFactory(client);
  }

  @Bean
  @Qualifier(BLAST)
  RestTemplate restTemplate(
    ObjectMapper objectMapper, OerPreparePacketHttpMessageConverter oerPreparePacketHttpMessageConverter,
    OkHttp3ClientHttpRequestFactory okHttp3ClientHttpRequestFactory
  ) {
    MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter(objectMapper);
    RestTemplate restTemplate = new RestTemplate(okHttp3ClientHttpRequestFactory);
    restTemplate.setMessageConverters(Lists.newArrayList(oerPreparePacketHttpMessageConverter, httpMessageConverter));
    return restTemplate;
  }

  @Bean
  HttpUrl defaultJwtTokenIssuer() {
    return Optional.ofNullable(environment.getProperty(DEFAULT_JWT_TOKEN_ISSUER))
      .map(HttpUrl::parse)
      .orElseThrow(() -> new IllegalStateException("Property `" + DEFAULT_JWT_TOKEN_ISSUER + "` must be defined!"));
  }

  @Bean
  BlastAccountIdResolver blastAccountIdResolver() {
    return new DefaultAccountIdResolver();
  }

  @PostConstruct
  public void startup() {
    linkFactoryProvider.registerLinkFactory(
      BlastLink.LINK_TYPE, new BlastLinkFactory(linkEventEmitter, blastRestTemplate, decryptor)
    );
  }
}
