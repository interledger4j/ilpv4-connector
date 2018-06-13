package com.sappenin.ilpv4.javax.money.providers;

import com.google.common.collect.Sets;
import org.javamoney.moneta.CurrencyUnitBuilder;

import javax.money.CurrencyQuery;
import javax.money.CurrencyUnit;
import javax.money.spi.CurrencyProviderSpi;
import java.util.Collections;
import java.util.Set;

/**
 * An implementation of {@link CurrencyProviderSpi} for registering a fictional currency called 'Dirt' that is
 * denominated in granules of dirt per single note.  The currency-code for this currency is "DRT".
 */
public class GuinCurrencyProvider implements CurrencyProviderSpi {

  public static final String GUIN = "GUIN";

  private Set<CurrencyUnit> currencyUnits;

  public GuinCurrencyProvider() {
    currencyUnits = Sets.newHashSet();
    currencyUnits.add(CurrencyUnitBuilder.of(GUIN, "GuinCurrencyBuilder")
      .setDefaultFractionDigits(2)
      .build());
    currencyUnits = Collections.unmodifiableSet(currencyUnits);
  }

  /**
   * Return a {@link CurrencyUnit} instances matching the given {@link javax.money.CurrencyQuery}.
   *
   * @param query the {@link javax.money.CurrencyQuery} containing the parameters determining the query. not null.
   *
   * @return the corresponding {@link CurrencyUnit}s matching, never null.
   */
  @Override
  public Set<CurrencyUnit> getCurrencies(CurrencyQuery query) {
    // only ensure DRT is the code, or it is a default query.
    if (query.isEmpty() || query.getCurrencyCodes().contains(GUIN) || query.getCurrencyCodes().isEmpty()) {
      return currencyUnits;
    }
    return Collections.emptySet();
  }
}
