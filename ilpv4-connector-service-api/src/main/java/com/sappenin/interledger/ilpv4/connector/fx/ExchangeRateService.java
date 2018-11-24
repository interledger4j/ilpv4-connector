package com.sappenin.interledger.ilpv4.connector.fx;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import java.math.BigInteger;

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

  /**
   * This method is called to allow statistics to be collected by the backend.
   *
   * @param updateRatePaymentParams
   */
  void logPaymentStats(UpdateRatePaymentParams updateRatePaymentParams);

  /**
   * A container object that collect all necessary information for {@link #logPaymentStats(UpdateRatePaymentParams)}.
   */
  @Value.Immutable
  interface UpdateRatePaymentParams {

    InterledgerAddress sourceAccountAddress();

    BigInteger sourceAmount();

    InterledgerAddress destinationAccountAddress();

    BigInteger destinationAmount();
  }
}
