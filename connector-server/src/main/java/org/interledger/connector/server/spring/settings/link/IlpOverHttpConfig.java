package org.interledger.connector.server.spring.settings.link;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.ILP_OVER_HTTP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.interledger.connector.server.spring.settings.web.JacksonConfig.PROBLEM;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.DefaultAccountIdResolver;
import org.interledger.connector.accounts.IlpOverHttpAccountIdResolver;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.IlpOverHttpConnectionSettings;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
  @Qualifier(PROBLEM)
  private ObjectMapper objectMapper;

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient ilpOverHttpClient;

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  @Autowired
  private Decryptor decryptor;

  /**
   * A bean for {@link IlpOverHttpConnectionSettings}, used to create an IlpOverHttp {@link OkHttpClient}
   * @param connectorSettings   A {@link Supplier<ConnectorSettings>} which include connection settings
   *                            for IlpOverHttp clients.
   * @return connection settings for {@link OkHttpClient}
   */
  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected IlpOverHttpConnectionSettings connectionSettings(Supplier<ConnectorSettings> connectorSettings) {
    return connectorSettings.get().ilpOverHttpSettings().connectionDefaults();
  }

  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected ConnectionPool ilpOverHttpConnectionPool(IlpOverHttpConnectionSettings connectionSettings) {
    return new ConnectionPool(
      connectionSettings.maxIdleConnections(),
      connectionSettings.keepAliveSeconds(), TimeUnit.SECONDS
    );
  }

  /**
   * A Bean for {@link OkHttp3ClientHttpRequestFactory}.
   *
   * @param ilpOverHttpConnectionPool   A {@link ConnectionPool} as configured above.
   * @param connectionSettings          A {@link IlpOverHttpConnectionSettings} with the following properties
   *
   * @return A {@link OkHttp3ClientHttpRequestFactory}.
   */
  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected OkHttpClient ilpOverHttpClient(
    @Qualifier(ILP_OVER_HTTP) final ConnectionPool ilpOverHttpConnectionPool,
    IlpOverHttpConnectionSettings connectionSettings
  ) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(connectionSettings.maxRequests());
    dispatcher.setMaxRequestsPerHost(connectionSettings.maxRequestsPerHost());
    builder.dispatcher(dispatcher);
    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(connectionSettings.connectTimeoutMillis(), TimeUnit.MILLISECONDS);
    builder.readTimeout(connectionSettings.readTimeoutMillis(), TimeUnit.MILLISECONDS);
    builder.writeTimeout(connectionSettings.writeTimeoutMillis(), TimeUnit.MILLISECONDS);

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
