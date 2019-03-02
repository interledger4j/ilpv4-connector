package com.sappenin.interledger.ilpv4.connector.fx;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.MonetaryConversions;
import java.util.Objects;

/**
 * An implementation of {@link ExchangeRateService} that sources all FX rates from Java Money.
 */
public class JavaMoneyExchangeRateService implements ExchangeRateService {

  /**
   * Computes the current exchange rate between the specified instances of {@link CurrencyUnit}.
   *
   * @param sourceAmount        The {@link MonetaryAmount} to convert.
   * @param destinationCurrency The {@link CurrencyUnit} to convert the {@code sourceAmount} into.
   *
   * @return
   */
  @Override
  public MonetaryAmount convert(final MonetaryAmount sourceAmount, final CurrencyUnit destinationCurrency) {
    Objects.requireNonNull(sourceAmount);
    Objects.requireNonNull(destinationCurrency);

    final CurrencyConversion currencyConversion = MonetaryConversions.getConversion(destinationCurrency);

    return sourceAmount.with(currencyConversion);
  }

  /**
   * This method is called to allow statistics to be collected by the backend.
   *
   * @param updateRatePaymentParams
   */
  @Override
  public void logPaymentStats(UpdateRatePaymentParams updateRatePaymentParams) {
    // No-op by default.
  }
}
