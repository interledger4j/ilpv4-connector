package com.sappenin.javax.money.providers;

import com.google.common.collect.Sets;
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

  public static final String XRP = "XRP";
  private Set<CurrencyUnit> currencyUnits;

  public XrpCurrencyProvider() {
    currencyUnits = Sets.newHashSet();
    currencyUnits.add(CurrencyUnitBuilder.of(XRP, "XrpCurrencyBuilder")
      .setDefaultFractionDigits(2)
      .build());
    currencyUnits = Collections.unmodifiableSet(currencyUnits);
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
