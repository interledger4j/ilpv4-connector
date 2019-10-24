package org.interledger.connector.server.spring.settings.blast;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.connector.core.ConfigConstants.BLAST_ENABLED;
import static org.interledger.connector.core.ConfigConstants.DEFAULT_JWT_TOKEN_ISSUER;
import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.TRUE;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.DefaultAccountIdResolver;
import org.interledger.connector.accounts.IlpOverHttpAccountIdResolver;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.events.LinkConnectionEventEmitter;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

/**
 * <p>Configures ILP-over-HTTP (i.e., BLAST), which provides a single Link-layer mechanism for this Connector's
 * peers.</p>
 *
 * <p>This type of Connector supports a single HTTP/2 Server endpoint in addition to multiple, statically configured,
 * BLAST client links.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = BLAST_ENABLED, havingValue = TRUE)
public class IlpOverHttpConfig {

  /**
   * @see "https://github.com/sappenin/java-ilpv4-connector/issues/360"
   * @deprecated See #360
   */
  @Deprecated
  public static final String BLAST = "blast";

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  Environment environment;

  @Autowired
  @Qualifier(BLAST)
  OkHttpClient ilpOverHttpClient;

  @Autowired
  LinkFactoryProvider linkFactoryProvider;

  @Autowired
  Decryptor decryptor;

  @Bean
  @Qualifier(BLAST)
  public ConnectionPool blastConnectionPool(
      @Value("${interledger.connector.ilpOverHttp.connectionDefaults.maxIdleConnections:5}") final int defaultMaxIdleConnections,
      @Value("${interledger.connector.ilpOverHttp.connectionDefaults.keepAliveMinutes:1}") final long defaultConnectionKeepAliveMinutes
  ) {
    return new ConnectionPool(
        defaultMaxIdleConnections,
        defaultConnectionKeepAliveMinutes, TimeUnit.MINUTES
    );
  }

  /**
   * A Bean for {@link OkHttp3ClientHttpRequestFactory}.
   *
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
  OkHttpClient ilpOverHttpClient(
      @Qualifier(BLAST) final ConnectionPool ilpOverHttpConnectionPool,
      @Value("${interledger.connector.ilpOverHttp.connectionDefaults.connectTimeoutMillis:1000}") final long defaultConnectTimeoutMillis,
      @Value("${interledger.connector.ilpOverHttp.connectionDefaults.readTimeoutMillis:60000}") final long defaultReadTimeoutMillis,
      @Value("${interledger.connector.ilpOverHttp.connectionDefaults.writeTimeoutMillis:60000}") final long defaultWriteTimeoutMillis
  ) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(defaultConnectTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.readTimeout(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.writeTimeout(defaultWriteTimeoutMillis, TimeUnit.MILLISECONDS);

    return builder.connectionPool(ilpOverHttpConnectionPool).build();
  }

  /**
   * A Bean for {@link OkHttp3ClientHttpRequestFactory}.
   *
   * @param okHttpClient A {@link OkHttpClient} to use in the Request factory.
   *
   * @return A {@link OkHttp3ClientHttpRequestFactory}.
   */
  @Bean
  @Qualifier(BLAST)
  OkHttp3ClientHttpRequestFactory ilpOverHttpClientHttpRequestFactory(
      @Qualifier(BLAST) final OkHttpClient okHttpClient
  ) {
    return new OkHttp3ClientHttpRequestFactory(okHttpClient);
  }

  @Bean
  HttpUrl defaultJwtTokenIssuer() {
    return Optional.ofNullable(environment.getProperty(DEFAULT_JWT_TOKEN_ISSUER))
        .map(HttpUrl::parse)
        .orElseThrow(() -> new IllegalStateException("Property `" + DEFAULT_JWT_TOKEN_ISSUER + "` must be defined!"));
  }

  @Bean
  IlpOverHttpAccountIdResolver blastAccountIdResolver() {
    return new DefaultAccountIdResolver();
  }

  @PostConstruct
  public void startup() {
    org.interledger.link.http.auth.Decryptor linkDecryptor = cipherMessage -> decryptor.decrypt(
        EncryptedSecret.fromEncodedValue(Base64.getEncoder().encodeToString(cipherMessage))
    );

    linkFactoryProvider.registerLinkFactory(
        IlpOverHttpLink.LINK_TYPE,
        new IlpOverHttpLinkFactory(
            ilpOverHttpClient, linkDecryptor, objectMapper, InterledgerCodecContextFactory.oer()
        )
    );
  }
}
