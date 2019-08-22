package org.interledger.connector.javax.money.providers;

import com.google.common.collect.ImmutableSet;
import org.javamoney.moneta.CurrencyUnitBuilder;

import javax.money.CurrencyQuery;
import javax.money.CurrencyUnit;
import javax.money.spi.CurrencyProviderSpi;
import java.util.Collections;
import java.util.Set;

/**
 * An implementation of {@link CurrencyProviderSpi} for registering XRP.
 */
public class XrpCurrencyProvider implements CurrencyProviderSpi {

  public static final String DROP = "DROP";
  public static final String XRP = "XRP";

  private Set<CurrencyUnit> currencyUnits;

  public XrpCurrencyProvider() {
    this.currencyUnits = ImmutableSet.<CurrencyUnit>builder()
      .add(
        CurrencyUnitBuilder.of(XRP, "XrpCurrencyProvider")
          .setDefaultFractionDigits(3) // XRP is generally modelled in the thousanths (but rounding is to the millionth)
          .build()
      )
      .build();
  }

  /**
   * Return a {@link CurrencyUnit} instances matching the given {@link CurrencyQuery}.
   *
   * @param query the {@link CurrencyQuery} containing the parameters determining the query. not null.
   *
   * @return the corresponding {@link CurrencyUnit}s matching, never null.
   */
  @Override
  public Set<CurrencyUnit> getCurrencies(CurrencyQuery query) {
    // only ensure DRT is the code, or it is a default query.
    if (query.isEmpty() || query.getCurrencyCodes().contains(XRP)) {
      return currencyUnits;
    }
    return Collections.emptySet();
  }
}
