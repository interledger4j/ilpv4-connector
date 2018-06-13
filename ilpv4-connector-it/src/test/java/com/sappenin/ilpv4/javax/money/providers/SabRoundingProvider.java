package com.sappenin.ilpv4.javax.money.providers;

import com.google.common.collect.ImmutableSet;

import javax.money.*;
import javax.money.spi.RoundingProviderSpi;
import java.math.BigDecimal;
import java.util.Set;

import static com.sappenin.ilpv4.javax.money.providers.SabCurrencyProvider.SAB;

/**
 * An implementation of {@link RoundingProviderSpi} for rounding tokens of type {@link SabCurrencyProvider#SAB}.
 */
public class SabRoundingProvider implements RoundingProviderSpi {

  private final Set<String> roundingNames = ImmutableSet.of(SAB);
  private final MonetaryRounding SABRounding = new SABRounding();

  public SabRoundingProvider() {
  }

  public MonetaryRounding getRounding(RoundingQuery query) {
    final CurrencyUnit currency = query.getCurrency();
    if (currency != null && (SAB.equals(currency.getCurrencyCode()))) {
      return SABRounding;
    } else if (SAB.equals(query.getRoundingName())) {
      return SABRounding;
    }
    return null;
  }

  public Set<String> getRoundingNames() {
    return roundingNames;
  }

  public static final class SABRounding implements MonetaryRounding {
    @Override
    public RoundingContext getRoundingContext() {
      return RoundingContextBuilder.of("SABRoundingProvider", SAB).build();
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