package org.interledger.connector.javax.money.providers;

import org.interledger.connector.javax.money.providers.DropRoundingProvider;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.MonetaryRounding;
import javax.money.RoundingQueryBuilder;
import javax.money.UnknownCurrencyException;
import java.math.BigDecimal;

import static org.interledger.connector.javax.money.providers.XrpCurrencyProvider.DROP;
import static org.interledger.connector.javax.money.providers.XrpCurrencyProvider.XRP;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link DropRoundingProvider}.
 */
public class DropRoundingProviderTest {

  private DropRoundingProvider dropRoundingProvider;

  @Before
  public void setUp() {
    dropRoundingProvider = new DropRoundingProvider();
  }

  @Test
  public void testRoundDropsWithAllDecimals() {
    final MonetaryAmount xrpAmount = Monetary.getAmountFactory(Money.class)
      .setCurrency(XRP)
      .setNumber(new BigDecimal("0.123456789"))
      .create();

    // round it to Drops
    final MonetaryAmount xrpRounded = xrpAmount.with(Monetary.getRounding(xrpAmount.getCurrency()));
    assertThat(xrpRounded.getNumber().numberValue(BigDecimal.class), is(new BigDecimal("0.123457")));
  }

  @Test
  public void testRoundDropsWith1Xrp() {
    final Money xrpAmount = Monetary.getAmountFactory(Money.class)
      .setCurrency(XRP)
      .setNumber(new BigDecimal("1.123456789"))
      .create();

    // round it to Drops
    final MonetaryAmount xrpRounded = xrpAmount.with(Monetary.getRounding(xrpAmount.getCurrency()));
    assertThat(xrpRounded.getNumber().numberValue(BigDecimal.class), is(new BigDecimal("1.123457")));
  }

  @Test
  public void testRoundDrops() {
    final MonetaryAmount xrpAmount = Monetary.getAmountFactory(Money.class)
      .setCurrency(XRP)
      .setNumber(new BigDecimal("221.123456"))
      .create();

    // round it to Drops
    final MonetaryAmount xrpRounded = xrpAmount.with(Monetary.getRounding(xrpAmount.getCurrency()));
    assertThat(xrpRounded.getNumber().numberValue(BigDecimal.class), is(new BigDecimal("221.123456")));
  }

  @Test
  public void getRounding() {
    final MonetaryRounding rounding = dropRoundingProvider.getRounding(RoundingQueryBuilder.of()
      .setCurrency(Monetary.getCurrency(XRP))
      //.set("cashRounding", true)
      .build());
    assertThat(rounding.getRoundingContext().getProviderName(), is("DropsProvider"));
    assertThat(rounding.getRoundingContext().getRoundingName(), is(DROP));
  }

  @Test(expected = UnknownCurrencyException.class)
  public void getRoundingNotFound() {
    try {
      dropRoundingProvider.getRounding(RoundingQueryBuilder.of()
        .setCurrency(Monetary.getCurrency("Foo"))
        .build());
    } catch (UnknownCurrencyException e) {
      assertThat(e.getMessage(), is("Unknown currency code: Foo"));
      throw e;
    }
  }

  @Test
  public void getRoundingNames() {
    assertThat(dropRoundingProvider.getRoundingNames().size(), is(1));
    assertThat(dropRoundingProvider.getRoundingNames().contains(DROP), is(true));
  }
}
