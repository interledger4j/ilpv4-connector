package com.sappenin.interledger.ilpv4.connector.javax.money.providers;

import com.google.common.collect.ImmutableSet;

import javax.money.*;
import javax.money.spi.RoundingProviderSpi;
import java.math.BigDecimal;
import java.util.Set;

import static com.sappenin.interledger.ilpv4.connector.javax.money.providers.GuinCurrencyProvider.GUIN;

/**
 * An implementation of {@link RoundingProviderSpi} for rounding tokens of type {@link GuinCurrencyProvider#GUIN}.
 */
public class GuinRoundingProvider implements RoundingProviderSpi {

  private final Set<String> roundingNames = ImmutableSet.of(GUIN);
  private final MonetaryRounding guinRounding = new GuinRounding();

  public GuinRoundingProvider() {
  }

  public MonetaryRounding getRounding(RoundingQuery query) {
    final CurrencyUnit currency = query.getCurrency();
    if (currency != null && (GUIN.equals(currency.getCurrencyCode()))) {
      return guinRounding;
    } else if (GUIN.equals(query.getRoundingName())) {
      return guinRounding;
    }
    return null;
  }

  public Set<String> getRoundingNames() {
    return roundingNames;
  }

  public static final class GuinRounding implements MonetaryRounding {
    @Override
    public RoundingContext getRoundingContext() {
      return RoundingContextBuilder.of("GuinRoundingProvider", GUIN).build();
    }

    @Override
    public MonetaryAmount apply(MonetaryAmount amount) {
      if (amount.getNumber().getNumberType().equals(BigDecimal.class)) {

        // Round the BigDecimal...
        final BigDecimal bd = amount.getNumber().numberValue(BigDecimal.class);

        final MonetaryContext mc = MonetaryContextBuilder.of()
          .setMaxScale(2)
          .setPrecision(0)
          .setFixedScale(true)
          .build();
        final MonetaryAmount newAmount = amount.getFactory()
          .setCurrency(amount.getCurrency())
          .setNumber(bd.setScale(2, BigDecimal.ROUND_HALF_UP))
          .setContext(mc)
          .create();
        return newAmount;
      } else {
        // TODO: Account for other Money types, like FastMoney...
        return amount;
      }
    }
  }
}