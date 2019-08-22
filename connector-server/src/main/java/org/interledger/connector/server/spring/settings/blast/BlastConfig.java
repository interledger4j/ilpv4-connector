package org.interledger.connector.server.spring.settings.blast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.interledger.connector.accounts.BlastAccountIdResolver;
import org.interledger.connector.accounts.DefaultAccountIdResolver;
import org.interledger.connector.server.spring.controllers.converters.OerPreparePacketHttpMessageConverter;
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

import static org.interledger.connector.core.ConfigConstants.BLAST_ENABLED;
import static org.interledger.connector.core.ConfigConstants.DEFAULT_JWT_TOKEN_ISSUER;
import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.connector.core.ConfigConstants.TRUE;

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

  /**
   * @see "https://github.com/sappenin/java-ilpv4-connector/issues/221"
   * @deprecated See #221
   */
  @Deprecated
  public static final String BLAST = "blast";

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
  public ConnectionPool blastConnectionPool(
    @Value("${interledger.connector.ilpOverHttp.connectionDefaults.maxIdleConnections:5}")
    final int defaultMaxIdleConnections,
    @Value("${interledger.connector.ilpOverHttp.connectionDefaults.keepAliveMinutes:1}")
    final long defaultConnectionKeepAliveMinutes
  ) {
    return new ConnectionPool(
      defaultMaxIdleConnections,
      defaultConnectionKeepAliveMinutes, TimeUnit.MINUTES
    );
  }

  /**
   * @param ilpOverHttpConnectionPool   A {@link ConnectionPool} as configured above.
   * @param defaultConnectTimeoutMillis Applied when connecting a TCP socket to the target host. A value of 0 means no
   *                                    timeout, otherwise values must be between 1 and {@link Integer#MAX_VALUE} when
   *                                    converted to milliseconds. If unspecified, defaults to 10000.
   * @param defaultReadTimeoutMillis    Applied to both the TCP socket and for individual read IO operations. A value of
   *                                    0 means no timeout, otherwise values must be between 1 and {@link
   *                                    Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to
   *                                    60000.
   * @param defaultWriteTimeoutMillis   Applied to individual write IO operations. A value of 0 means no timeout,
   *                                    otherwise values must be between 1 and {@link Integer#MAX_VALUE} when converted
   *                                    to milliseconds. If unspecified, defaults to 60000.
   *
   * @return A {@link OkHttp3ClientHttpRequestFactory}.
   */
  @Bean
  @Qualifier(BLAST)
  OkHttp3ClientHttpRequestFactory blastOkHttp3ClientHttpRequestFactory(

    @Qualifier(BLAST) final ConnectionPool ilpOverHttpConnectionPool,

    @Value("${interledger.connector.ilpOverHttp.connectionDefaults.connectTimeoutMillis:1000}")
    final long defaultConnectTimeoutMillis,
    @Value("${interledger.connector.ilpOverHttp.connectionDefaults.readTimeoutMillis:60000}")
    final long defaultReadTimeoutMillis,
    @Value("${interledger.connector.ilpOverHttp.connectionDefaults.writeTimeoutMillis:60000}")
    final long defaultWriteTimeoutMillis
  ) {
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
  RestTemplate blastRestTemplate(
    ObjectMapper objectMapper,
    OerPreparePacketHttpMessageConverter oerPreparePacketHttpMessageConverter,
    @Qualifier(BLAST) OkHttp3ClientHttpRequestFactory okHttp3ClientHttpRequestFactory
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
