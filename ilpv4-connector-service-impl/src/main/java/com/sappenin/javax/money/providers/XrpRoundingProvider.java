package com.sappenin.javax.money.providers;

import com.google.common.collect.ImmutableSet;

import javax.money.*;
import javax.money.spi.RoundingProviderSpi;
import java.math.BigDecimal;
import java.util.Set;

import static com.sappenin.javax.money.providers.XrpCurrencyProvider.XRP;

/**
 * An implementation of {@link RoundingProviderSpi} for rounding tokens of type {@link XrpCurrencyProvider#XRP}.
 */
public class XrpRoundingProvider implements RoundingProviderSpi {

  private final Set<String> roundingNames = ImmutableSet.of(XRP);
  private final MonetaryRounding xrpRounding = new XrpRounding();

  public XrpRoundingProvider() {
  }

  public MonetaryRounding getRounding(RoundingQuery query) {
    final CurrencyUnit currency = query.getCurrency();
    if (currency != null && (XRP.equals(currency.getCurrencyCode()))) {
      return xrpRounding;
    } else if (XRP.equals(query.getRoundingName())) {
      return xrpRounding;
    }
    return null;
  }

  public Set<String> getRoundingNames() {
    return roundingNames;
  }

  public static final class XrpRounding implements MonetaryRounding {
    @Override
    public RoundingContext getRoundingContext() {
      return RoundingContextBuilder.of("XrpRoundingProvider", XRP).build();
    }

    @Override
    public MonetaryAmount apply(MonetaryAmount amount) {
      if (amount.getNumber().getNumberType().equals(BigDecimal.class)) {

        // Round the BigDecimal...
        final BigDecimal bd = amount.getNumber().numberValue(BigDecimal.class);

        final MonetaryContext mc = MonetaryContextBuilder.of()
          .setMaxScale(9)
          .setPrecision(0)
          .setFixedScale(true)
          .build();
        final MonetaryAmount newAmount = amount.getFactory()
          .setCurrency(amount.getCurrency())
          .setNumber(bd.setScale(9, BigDecimal.ROUND_HALF_UP))
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