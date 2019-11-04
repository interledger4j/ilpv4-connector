package org.interledger.connector.javax.money.providers;

import org.interledger.connector.caching.FxRateOverridesLoadingCache;
import org.interledger.connector.fxrates.FxRateOverride;

import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.AbstractRateProvider;
import org.javamoney.moneta.spi.DefaultNumberValue;

import javax.money.CurrencyUnit;
import javax.money.convert.ConversionQuery;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.ProviderContext;
import javax.money.convert.ProviderContextBuilder;
import javax.money.convert.RateType;

/**
 * A {@link ExchangeRateProvider} that loads FX data from user specified overrides.
 *
 * @see "https://github.com/JavaMoney/javamoney-lib/blob/master/exchange/exchange-rate-frb/src/main/java/org/javamoney/
 * moneta/convert/frb/USFederalReserveRateProvider.java"
 */
public class FxRateOverridesProvider extends AbstractRateProvider {

  private static final ProviderContext CONTEXT = ProviderContextBuilder.of("OVERRIDE", RateType.OTHER)
      .set("providerDescription", "User specified overrides to FX rates.").build();

  private final FxRateOverridesLoadingCache cache;

  /**
   *
   * @param cache the underlying loading cache for overrides
   */
  public FxRateOverridesProvider(FxRateOverridesLoadingCache cache) {
    super(CONTEXT);
    this.cache = cache;
  }

  @Override
  public ExchangeRate getExchangeRate(ConversionQuery conversionQuery) {
    CurrencyUnit from = conversionQuery.getBaseCurrency();
    CurrencyUnit to = conversionQuery.getCurrency();

    String key = from.getCurrencyCode() + "-" + to.getCurrencyCode();
    FxRateOverride override = cache.getOverride(key);

    // cache miss (most requests will be a miss)
    if (override == null) {
      return null;
    }

    ExchangeRate rateOverride = exchangeRateBuilder(conversionQuery)
        .setFactor(DefaultNumberValue.of(override.rate()))
        .build();
    return rateOverride;
  }

  private ExchangeRateBuilder exchangeRateBuilder(ConversionQuery query) {
    // TODO: This maps to scale, but shouldn't be hard-coded to "6" in javamoney.properties.
    ExchangeRateBuilder builder = new ExchangeRateBuilder(getExchangeContext("cc.digit.fraction"));
    builder.setBase(query.getBaseCurrency());
    builder.setTerm(query.getCurrency());
    return builder;
  }
}
