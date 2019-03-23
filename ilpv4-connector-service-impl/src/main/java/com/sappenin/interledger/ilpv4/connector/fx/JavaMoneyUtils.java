package com.sappenin.interledger.ilpv4.connector.fx;

import org.interledger.core.InterledgerPreparePacket;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Interledger amounts are integers, but most currencies are typically represented as fractional units, e.g. cents. This
 * class helps perform various calculations and transformation to map between Interledger amounts and JavaMoney
 * primitives.
 */
public class JavaMoneyUtils {

  /**
   * Take an amount (typically sourced from an {@link InterledgerPreparePacket}) and convert it to a proper {@link
   * MonetaryAmount} that can work with JavaMoney.
   *
   * @param assetAmount A {@link BigInteger} representing a total number of units.
   * @param assetScale  The order of magnitude used to express one full currency unit in the currency's base units. More
   *                    formally, an integer (..., -2, -1, 0, 1, 2, ...), such that one of the base units equals
   *                    <tt>1*10^(-currencyScale)</tt>. For example, 10000 dollar-cents (assetScale is 2) would be
   *                    translated into dollars via the following equation: `10000 * (10^(-2))`, or `100.00`.
   *
   * @return
   */
  public MonetaryAmount toMonetaryAmount(
    final CurrencyUnit currencyUnit, final BigInteger assetAmount, final int assetScale
  ) {
    // 12345 units in USD is $123.45

    // BigDecimal.valueOf performs scaling automatically...
    final BigDecimal scaledAmount = BigDecimal.valueOf(assetAmount.longValue(), assetScale);
    return Money.of(scaledAmount, currencyUnit);
  }

  /**
   * * Take an amount (typically sourced from an JavaMoney) and convert it to a proper {@link BigInteger} that can work
   * with Interledger.
   *
   * @param monetaryAmount A {@link MonetaryAmount} representing a value in a particular currency.
   * @param assetScale     The order of magnitude used to express one full currency unit in the currency's base units.
   *                       More formally, an integer (..., -2, -1, 0, 1, 2, ...), such that one of the base units
   *                       equals
   *                       <tt>1*10^(-currencyScale)</tt>. For example, 10000 dollar-cents (assetScale is 2) would be
   *                       translated into dollars via the following equation: `10000 * (10^(-2))`, or `100.00`.
   *
   * @return
   */
  public BigInteger toInterledgerAmount(MonetaryAmount monetaryAmount, int assetScale) {
    // 123.45 --> 12345 if scale is 2

    return monetaryAmount.scaleByPowerOfTen(assetScale).getNumber().numberValue(BigDecimal.class)
      // Throw an exception if any precision is lost.
      .toBigIntegerExact();
  }
}
