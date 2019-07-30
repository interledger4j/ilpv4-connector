package com.sappenin.interledger.ilpv4.connector.server.spring.settings.javamoney;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.fx.JavaMoneyUtils;
import com.sappenin.interledger.javax.money.providers.CryptoCompareRateProvider;
import com.sappenin.interledger.javax.money.providers.DropRoundingProvider;
import com.sappenin.interledger.javax.money.providers.XrpCurrencyProvider;
import org.javamoney.moneta.internal.DefaultRoundingProvider;
import org.javamoney.moneta.internal.JDKCurrencyProvider;
import org.javamoney.moneta.internal.convert.DefaultMonetaryConversionsSingletonSpi;
import org.javamoney.moneta.internal.convert.IdentityRateProvider;
import org.javamoney.moneta.spi.CompoundRateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.convert.ExchangeRateProvider;
import javax.money.spi.RoundingProviderSpi;
import java.util.function.Supplier;

import static com.sappenin.interledger.javax.money.providers.XrpCurrencyProvider.XRP;

/**
 * Configures JavaMoney beans so they can be connected into the JavaMoney subsystem using
 * <tt>SpringServiceProvider</tt>.
 */
@Configuration
public class JavaMoneyConfig {

  private static final String DEFAULT = "default";
  private static final String CRYPTO_COMPARE = "cryptocompare";
  private static final String FX = "fx";
  private static final String DROP = "DROP";

  @Autowired
  Environment environment;

  @PostConstruct
  public void setup() {
    // Sanity check to ensure that XRP FX is configured properly...this will throw an exception if configuration is
    // wrong.
    CurrencyUnit xrp = Monetary.getCurrency(XRP);
    Preconditions.checkNotNull(xrp != null);
    CurrencyUnit usd = Monetary.getCurrency("USD");
    Preconditions.checkNotNull(usd != null);
  }

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


  @Bean
  DefaultMonetaryConversionsSingletonSpi defaultMonetaryConversionsSingletonSpi() {
    return new DefaultMonetaryConversionsSingletonSpi();
  }

  @PostConstruct
  public void init() {


  }
}
