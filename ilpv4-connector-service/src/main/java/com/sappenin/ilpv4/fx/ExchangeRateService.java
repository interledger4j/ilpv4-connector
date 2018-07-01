package com.sappenin.ilpv4.fx;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;

public interface ExchangeRateService {

  /**
   * Computes the current exchange rate between the specified instances of {@link CurrencyUnit}.
   *
   * @param sourceCurrencyUnit
   * @param destinationCurrencyUnit
   * @return
   */
  // TODO: Consider removing this, or else implement it?
  //ExchangeRateInfo getExchangeRate(CurrencyUnit sourceCurrencyUnit, CurrencyUnit destinationCurrencyUnit);

  /**
   * Computes the current exchange rate between the specified instances of {@link CurrencyUnit}.
   *
   * @param sourceAmount            The {@link MonetaryAmount} to convert.
   * @param destinationCurrencyUnit The {@link CurrencyUnit} to convert the {@code sourceAmount} to.
   *
   * @return
   */
  MonetaryAmount convert(MonetaryAmount sourceAmount, CurrencyUnit destinationCurrencyUnit);

}
