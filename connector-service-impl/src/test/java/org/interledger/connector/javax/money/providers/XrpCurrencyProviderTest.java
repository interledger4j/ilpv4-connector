package org.interledger.connector.javax.money.providers;

import org.interledger.connector.javax.money.providers.XrpCurrencyProvider;
import org.junit.Before;
import org.junit.Test;

import javax.money.CurrencyQueryBuilder;
import javax.money.CurrencyUnit;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import static org.interledger.connector.javax.money.providers.XrpCurrencyProvider.DROP;
import static org.interledger.connector.javax.money.providers.XrpCurrencyProvider.XRP;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link XrpCurrencyProvider}.
 */
public class XrpCurrencyProviderTest {

  private XrpCurrencyProvider provider;

  @Before
  public void setUp() {
    provider = new XrpCurrencyProvider();
  }

  @Test
  public void getCurrencies() {
    final Set<CurrencyUnit> currencyUnits = provider.getCurrencies(
      CurrencyQueryBuilder.of()
        .setCurrencyCodes(XRP)
        .setCountries(Locale.CANADA) // Land
        .set(LocalDate.of(1970, 1, 1)) // Datum
        .setProviderNames(XRP).build() // Provider
    );

    assertThat(currencyUnits.size(), is(1));
    assertThat(currencyUnits.stream().findFirst().get().getCurrencyCode(), is(XRP));
  }

  @Test
  public void getCurrenciesUnknownProvider() {
    final Set<CurrencyUnit> currencyUnits = provider.getCurrencies(
      CurrencyQueryBuilder.of()
        .setCountries(Locale.CANADA) // Land
        .set(LocalDate.of(1970, 1, 1)) // Datum
        .setProviderNames(DROP).build() // Provider
    );

    assertThat(currencyUnits.size(), is(0));
  }
}
