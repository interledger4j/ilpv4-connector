package org.interledger.connector.server.spring.settings.javamoney;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.connector.javax.money.providers.XrpCurrencyProvider.XRP;

import org.interledger.connector.fx.JavaMoneyUtils;
import org.interledger.connector.javax.money.providers.CryptoCompareRateProvider;
import org.interledger.connector.javax.money.providers.DropRoundingProvider;
import org.interledger.connector.javax.money.providers.XrpCurrencyProvider;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.FxConnectionSettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.Lists;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import org.javamoney.moneta.convert.internal.DefaultMonetaryConversionsSingletonSpi;
import org.javamoney.moneta.convert.internal.IdentityRateProvider;
import org.javamoney.moneta.internal.DefaultRoundingProvider;
import org.javamoney.moneta.internal.JDKCurrencyProvider;
import org.javamoney.moneta.spi.CompoundRateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.money.convert.ConversionQuery;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.spi.RoundingProviderSpi;

/**
 * Configures JavaMoney.
 *
 * Note that it is technically possible to connect Spring bean implementations of the JavaMoney SPI (i.e., {@link
 * javax.money.spi.ServiceProvider} into the JavaMoney bootstrapping framework. One such attempt was using {@link
 * SpringServiceProvider} although it doesn't quite work properly, so is current disabled in the SPI file called
 * `javax.money.spi.ServiceProvider`.
 */
@Configuration
public class JavaMoneyConfig {

  private static final String DEFAULT = "default";
  private static final String CRYPTO_COMPARE = "cryptocompare";
  private static final String FX = "fx";
  private static final String DROP = "DROP";

  @Autowired
  private Environment environment;

  @Bean
  protected JavaMoneyUtils javaMoneyUtils() {
    return new JavaMoneyUtils();
  }

  @Bean
  protected ApplicationContextProvider applicationContextProvider() {
    return new ApplicationContextProvider();
  }

  /**
   * The {@link RestTemplate} used for all FX queries to various external services.
   */
  @Bean
  @Qualifier(FX)
  protected RestTemplate fxRestTemplate(ObjectMapper objectMapper,
      @Qualifier(FX) OkHttp3ClientHttpRequestFactory requestFactory) {
    final MappingJackson2HttpMessageConverter httpMessageConverter =
        new MappingJackson2HttpMessageConverter(objectMapper);
    RestTemplate restTemplate = new RestTemplate(requestFactory);
    restTemplate.setMessageConverters(Lists.newArrayList(httpMessageConverter));
    return restTemplate;
  }

  @Bean
  @Qualifier(CRYPTO_COMPARE)
  protected Supplier<String> apiKeySupplier() {
    return () -> environment.getProperty("cryptocompare.api.key");
  }

  @Bean
  protected IdentityRateProvider identityRateProvider() {
    return new IdentityRateProvider();
  }

  @Bean
  protected CryptoCompareRateProvider cryptoCompareRateProvider(
      @Qualifier(CRYPTO_COMPARE) Supplier<String> cryptoCompareApiKeySupplier,
      @Qualifier(FX) RestTemplate restTemplate,
      Cache<ConversionQuery, ExchangeRate> fxCache
  ) {
    return new CryptoCompareRateProvider(cryptoCompareApiKeySupplier, restTemplate, fxCache);
  }

  @Bean
  protected ExchangeRateProvider exchangeRateProvider(CryptoCompareRateProvider cryptoCompareRateProvider) {
    return new CompoundRateProvider(
        Lists.newArrayList(cryptoCompareRateProvider, new IdentityRateProvider())
    );
  }

  ////////////////////////
  // Rounding Provider SPI
  ////////////////////////

  @Bean
  @Qualifier(DROP)
  protected RoundingProviderSpi dropRoundingProvider() {
    return new DropRoundingProvider();
  }

  @Bean
  @Qualifier(DEFAULT)
  protected RoundingProviderSpi defaultRoundingProvider() {
    return new DefaultRoundingProvider();
  }

  ////////////////////////
  // Currency Code SPI
  ////////////////////////

  @Bean
  @Qualifier(XRP)
  protected XrpCurrencyProvider xrpCurrencyProviderSpi() {
    return new XrpCurrencyProvider();
  }

  @Bean
  @Qualifier(DEFAULT)
  protected JDKCurrencyProvider jdkCurrencyProvider() {
    return new JDKCurrencyProvider();
  }

  // NOTE: This bean must have this name in order to properly bridge with JavaMoney.
  @Bean("javax.money.spi.MonetaryCurrenciesSingletonSpi")
  protected DefaultMonetaryConversionsSingletonSpi defaultMonetaryConversionsSingletonSpi() {
    return new DefaultMonetaryConversionsSingletonSpi();
  }

  @Bean
  @Qualifier(FX)
  FxConnectionSettings fxConnectionSettings(Supplier<ConnectorSettings> connectorSettings) {
    return connectorSettings.get().fxSettings().connectionDefaults();
  }

  @Bean
  @Qualifier(FX)
  OkHttpClient fxHttpClient(
      @Qualifier(FX) final ConnectionPool fxConnectionPool,
      FxConnectionSettings fxConnectionSettings
  ) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(fxConnectionSettings.connectTimeoutMillis(), TimeUnit.MILLISECONDS);
    builder.readTimeout(fxConnectionSettings.readTimeoutMillis(), TimeUnit.MILLISECONDS);
    builder.writeTimeout(fxConnectionSettings.writeTimeoutMillis(), TimeUnit.MILLISECONDS);

    return builder.connectionPool(fxConnectionPool).build();
  }

  @Bean
  @Qualifier(FX)
  protected OkHttp3ClientHttpRequestFactory fxOkHttp3ClientHttpRequestFactory(
      @Qualifier(FX) final OkHttpClient fxHttpClient) {
    return new OkHttp3ClientHttpRequestFactory(fxHttpClient);
  }

  @Bean
  @Qualifier(FX)
  public ConnectionPool fxConnectionPool(FxConnectionSettings fxConnectionSettings) {
    return new ConnectionPool(
      fxConnectionSettings.maxIdleConnections(),
      fxConnectionSettings.keepAliveMinutes(), TimeUnit.MINUTES
    );
  }

}
