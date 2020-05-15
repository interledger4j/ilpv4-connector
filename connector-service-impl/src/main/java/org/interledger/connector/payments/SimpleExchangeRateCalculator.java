package org.interledger.connector.payments;

import org.interledger.stream.Denomination;
import org.interledger.stream.calculators.ExchangeRateCalculator;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * Simple exchange rate calculator that does no FX rate calculations.
 */
public class SimpleExchangeRateCalculator implements ExchangeRateCalculator {

  @Override
  public UnsignedLong calculateAmountToSend(UnsignedLong amountToSend,
                                            Denomination amountToSendDenomination,
                                            Denomination receiverDenomination) {
    return amountToSend;
  }

  @Override
  public UnsignedLong calculateMinAmountToAccept(
    final UnsignedLong sendAmount, final Denomination sendAmountDenomination
  ) {
    Objects.requireNonNull(sendAmount);
    Objects.requireNonNull(sendAmountDenomination);
    return UnsignedLong.ZERO; // must be zero because Quilt can send a very low value payment to start
  }

}
