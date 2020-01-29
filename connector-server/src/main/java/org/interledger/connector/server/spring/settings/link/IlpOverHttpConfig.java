package org.interledger.connector.server.spring.settings.link;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.ILP_OVER_HTTP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.DefaultAccountIdResolver;
import org.interledger.connector.accounts.IlpOverHttpAccountIdResolver;
import org.interledger.connector.server.spring.settings.Redactor;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
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
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

/**
 * <p>Configures ILP-over-HTTP, which provides a single Link-layer mechanism for this Connector's peers.</p>
 *
 * <p>This type of Connector supports a single HTTP/2 Server endpoint in addition to multiple, statically configured,
 * ILP-over-HTTP client links.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = ILP_OVER_HTTP_ENABLED, havingValue = TRUE)
public class IlpOverHttpConfig {

  public static final String ILP_OVER_HTTP = "ILP-over-HTTP";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient ilpOverHttpClient;

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  @Autowired
  private Decryptor decryptor;

  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected ConnectionPool ilpOverHttpConnectionPool(
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
  @Qualifier(ILP_OVER_HTTP)
  protected OkHttpClient ilpOverHttpClient(
    @Qualifier(ILP_OVER_HTTP) final ConnectionPool ilpOverHttpConnectionPool,
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
  @Qualifier(ILP_OVER_HTTP)
  protected OkHttp3ClientHttpRequestFactory ilpOverHttpClientHttpRequestFactory(
    @Qualifier(ILP_OVER_HTTP) final OkHttpClient okHttpClient
  ) {
    return new OkHttp3ClientHttpRequestFactory(okHttpClient);
  }

  @Bean
  protected IlpOverHttpAccountIdResolver ilpOverHttpAccountIdResolver() {
    return new DefaultAccountIdResolver();
  }

  @Bean
  protected Redactor redactor() {
    return new Redactor();
  }

  @PostConstruct
  public void startup() {
    // The value passed-in here as `encryptedConnectorPropertyStringBytes` will actually be an encrypted property as
    // encrypted via connector-crypto-cli. For testing purposes, reference the Connector properties for a given account.
    org.interledger.link.http.auth.Decryptor linkDecryptor = encryptedConnectorPropertyStringBytes -> decryptor.decrypt(
      EncryptedSecret.fromEncodedValue(new String(encryptedConnectorPropertyStringBytes))
    );

    linkFactoryProvider.registerLinkFactory(
      IlpOverHttpLink.LINK_TYPE,
      new IlpOverHttpLinkFactory(
        ilpOverHttpClient, linkDecryptor, objectMapper, InterledgerCodecContextFactory.oer()
      )
    );
  }
}
