package com.sappenin.interledger.javax.money.providers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.AbstractRateProvider;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import javax.money.Monetary;
import javax.money.MonetaryException;
import javax.money.convert.ConversionContext;
import javax.money.convert.ConversionQuery;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.ProviderContext;
import javax.money.convert.ProviderContextBuilder;
import javax.money.convert.RateType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.javamoney.moneta.spi.AbstractCurrencyConversion.KEY_SCALE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * A {@link ExchangeRateProvider} that loads FX data from CryptoCompare. This provider loads all available rates,
 * purging from its cache any older rates with each re-load.
 *
 * @see "https://min-api.cryptocompare.com/documentation"
 * @see "https://github.com/JavaMoney/javamoney-lib/blob/master/exchange/exchange-rate-frb/src/main/java/org/javamoney/
 * moneta/convert/frb/USFederalReserveRateProvider.java"
 */
public class CryptoCompareRateProvider extends AbstractRateProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoCompareRateProvider.class.getName());

  private static final ProviderContext CONTEXT = ProviderContextBuilder.of("CC", RateType.DEFERRED)
    .set("providerDescription", "CryptoCompare API Rate (https://min-api.cryptocompare.com)").build();

  private final LoadingCache<ConversionQuery, ExchangeRate> exchangeRateCache;

  private final Supplier<String> apiKeySupplier;
  private final RestTemplate restTemplate;
  private String apiUrlTemplate;

  // TODO: Add spread.

  // The spread is subtracted from the rate when going in either direction,
  // so that the DestinationAmount always ends up being slightly less than
  // the (equivalent) SourceAmount -- regardless of which of the 2 is fixed:
  //
  //   SourceAmount * Rate * (1 - Spread) = DestinationAmount
  //
  //    const rate = new BigNumber(destinationRate).shiftedBy(destinationInfo.assetScale)
  //      .div(new BigNumber(sourceRate).shiftedBy(sourceInfo.assetScale))
  //    .times(new BigNumber(1).minus(this.spread))
  //    .toPrecision(15)
  // '0.005'` Set the connector's spread to 0.5%. This is an example for how to pass configuration to the connector.


  public CryptoCompareRateProvider(final Supplier<String> apiKeySupplier, final RestTemplate restTemplate) {
    super(CONTEXT);
    this.apiKeySupplier = Objects.requireNonNull(apiKeySupplier);

    this.restTemplate = Objects.requireNonNull(restTemplate);

    // Sensible defaults (override with setter-injection)
    this.apiUrlTemplate =
      "https://min-api.cryptocompare.com/data/price?fsym={fsym}&tsyms={tsyms}&extraParams=java.ilpv4.connector";
    this.exchangeRateCache = this.fxLoader();

    Monetary.getCurrency("XRP");
  }

  private LoadingCache<ConversionQuery, ExchangeRate> fxLoader() {
    return CacheBuilder.newBuilder()
      //.maximumSize(100) // Not enabled for now in order to support many accounts.
      .expireAfterAccess(30, TimeUnit.SECONDS)
      .build(
        new CacheLoader<ConversionQuery, ExchangeRate>() {
          // Computes or retrieves the value corresponding to {@code conversionQuery}.
          @Override
          public ExchangeRate load(final ConversionQuery conversionQuery) {
            Objects.requireNonNull(conversionQuery);

            final ExchangeRateBuilder builder = exchangeRateBuilder(conversionQuery);
            final String baseCurrencyCode = conversionQuery.getBaseCurrency().getCurrencyCode();
            final String terminatingCurrencyCode = conversionQuery.getCurrency().getCurrencyCode();

            if (baseCurrencyCode.equals(terminatingCurrencyCode)) {
              builder.setFactor(DefaultNumberValue.ONE);
            } else {

              // In JavaMoney, the Base currency is the currency being dealt with, and the terminating currency is
              // the currency that the base is converted into. E.g., `XRP, in USD, is $0.3133`, then XRP would be the
              // base currency, and USD would be the terminating currency. In CryptoCompare, the `fsym` and `tsym`
              // map this relationship. We ask the API, convert `XRP` (fsym) into `USD` (tsym). We get a response
              // containing a map of values keyed by each `tsym`. So, we can map the `tsym` to the terminating currency.

              // Call Remote API to load the rate.
              final Map<String, String> ratesResponse = restTemplate.exchange(
                apiUrlTemplate, HttpMethod.GET, httpEntityWithCustomHeaders(),
                new ParameterizedTypeReference<Map<String, String>>() {
                },
                baseCurrencyCode, terminatingCurrencyCode
              ).getBody();

              Optional.ofNullable(ratesResponse.get(terminatingCurrencyCode))
                .map(value -> builder.setFactor(new DefaultNumberValue(new BigDecimal(value))))
                .orElseThrow(
                  () -> new RuntimeException(String.format("No Rate found for ConversionQuery: %s", conversionQuery))
                );
            }

            return builder.build();
          }
        }
      );
  }

  // Access a {@link ExchangeRate} using the given currencies.
  @Override
  public ExchangeRate getExchangeRate(ConversionQuery conversionQuery) {
    Objects.requireNonNull(conversionQuery);
    try {
      // TODO: Interface contract says "never-null" but all implementations return null. :(
      return this.exchangeRateCache.get(conversionQuery);
    } catch (Exception e) {
      throw new MonetaryException("Failed to load currency conversion data", e);
    }
  }

  private ExchangeRateBuilder exchangeRateBuilder(ConversionQuery query) {
    // TODO: This maps to scale, but shouldn't be hard-coded to "6" in javamoney.properties.
    ExchangeRateBuilder builder = new ExchangeRateBuilder(getExchangeContext("cc.digit.fraction"));
    builder.setBase(query.getBaseCurrency());
    builder.setTerm(query.getCurrency());
    return builder;
  }

  @Override
  protected ConversionContext getExchangeContext(String key) {
    int scale = getScale(key);
    if (scale < 0) {
      return ConversionContext.of(CONTEXT.getProviderName(), RateType.DEFERRED);
    } else {
      return ConversionContext.of(CONTEXT.getProviderName(), RateType.DEFERRED).toBuilder().set(KEY_SCALE, scale)
        .build();
    }
  }

  @VisibleForTesting
  protected HttpEntity<Map<String, String>> httpEntityWithCustomHeaders() {
    final HttpHeaders apiGetHeaders = new HttpHeaders();
    // Authorization: Apikey {your_api_key}=
    apiGetHeaders.set(AUTHORIZATION, String.format("Apikey %s", apiKeySupplier.get()));
    return new HttpEntity<>(null, apiGetHeaders);
  }

  public void setApiUrlTemplate(String apiUrlTemplate) {
    this.apiUrlTemplate = apiUrlTemplate;
  }
}