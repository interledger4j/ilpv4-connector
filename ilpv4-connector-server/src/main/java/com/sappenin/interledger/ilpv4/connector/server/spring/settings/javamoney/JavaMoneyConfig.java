package com.sappenin.interledger.ilpv4.connector.server.spring.settings.javamoney;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.fx.JavaMoneyUtils;
import com.sappenin.interledger.javax.money.providers.CryptoCompareRateProvider;
import com.sappenin.interledger.javax.money.providers.DropRoundingProvider;
import com.sappenin.interledger.javax.money.providers.XrpCurrencyProvider;
import org.javamoney.moneta.convert.internal.DefaultMonetaryConversionsSingletonSpi;
import org.javamoney.moneta.convert.internal.IdentityRateProvider;
import org.javamoney.moneta.internal.DefaultRoundingProvider;
import org.javamoney.moneta.internal.JDKCurrencyProvider;
import org.javamoney.moneta.spi.CompoundRateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.money.convert.ExchangeRateProvider;
import javax.money.spi.RoundingProviderSpi;
import java.util.function.Supplier;

import static com.sappenin.interledger.javax.money.providers.XrpCurrencyProvider.XRP;

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
  Environment environment;

  @Bean
  JavaMoneyUtils javaMoneyUtils() {
    return new JavaMoneyUtils();
  }

  @Bean
  ApplicationContextProvider applicationContextProvider() {
    return new ApplicationContextProvider();
  }

  /**
   * The {@link RestTemplate} used for all FX queries to various external services.
   */
  @Bean
  @Qualifier(FX)
  RestTemplate fxRestTemplate(ObjectMapper objectMapper) {
    final MappingJackson2HttpMessageConverter httpMessageConverter =
      new MappingJackson2HttpMessageConverter(objectMapper);
    return new RestTemplate(Lists.newArrayList(httpMessageConverter));
  }

  @Bean
  @Qualifier(CRYPTO_COMPARE)
  Supplier<String> apiKeySupplier() {
    return () -> environment.getProperty("cryptocompare.api.key");
  }

  @Bean
  IdentityRateProvider identityRateProvider() {
    return new IdentityRateProvider();
  }

  @Bean
  CryptoCompareRateProvider cryptoCompareRateProvider(
    @Qualifier(CRYPTO_COMPARE) Supplier<String> cryptoCompareApiKeySupplier,
    @Qualifier(FX) RestTemplate restTemplate
  ) {
    return new CryptoCompareRateProvider(cryptoCompareApiKeySupplier, restTemplate);
  }

  @Bean
  ExchangeRateProvider exchangeRateProvider(CryptoCompareRateProvider cryptoCompareRateProvider) {
    return new CompoundRateProvider(
      Lists.newArrayList(cryptoCompareRateProvider, new IdentityRateProvider())
    );
  }

  ////////////////////////
  // Rounding Provider SPI
  ////////////////////////

  @Bean
  @Qualifier(DROP)
  RoundingProviderSpi dropRoundingProvider() {
    return new DropRoundingProvider();
  }

  @Bean
  @Qualifier(DEFAULT)
  RoundingProviderSpi defaultRoundingProvider() {
    return new DefaultRoundingProvider();
  }

  ////////////////////////
  // Currency Code SPI
  ////////////////////////

    @Bean
    @Qualifier(XRP)
    XrpCurrencyProvider xrpCurrencyProviderSpi() {
      return new XrpCurrencyProvider();
    }

  @Bean
  @Qualifier(DEFAULT)
  JDKCurrencyProvider jdkCurrencyProvider() {
    return new JDKCurrencyProvider();
  }

  @Bean("javax.money.spi.MonetaryCurrenciesSingletonSpi")
  DefaultMonetaryConversionsSingletonSpi defaultMonetaryConversionsSingletonSpi() {
    return new DefaultMonetaryConversionsSingletonSpi();
  }
}
