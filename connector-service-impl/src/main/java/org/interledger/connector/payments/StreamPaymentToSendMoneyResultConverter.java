package org.interledger.connector.payments;

import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyResult;

import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.math.BigInteger;
import java.time.Duration;

public class StreamPaymentToSendMoneyResultConverter implements Converter<StreamPayment, SendMoneyResult> {

  @Override
  public SendMoneyResult convert(StreamPayment streamPayment) {
    return SendMoneyResult.builder()
      .amountDelivered(streamPayment.deliveredAmount())
      .destinationAddress(streamPayment.destinationAddress())
      .sendMoneyDuration(Duration.between(streamPayment.createdAt(), streamPayment.modifiedAt()))
      .originalAmount(streamPayment.expectedAmount().map(BigInteger::abs).map(UnsignedLong::valueOf)
        .orElse(UnsignedLong.ZERO))
      .numFulfilledPackets(streamPayment.packetCount())
      .amountSent(UnsignedLong.valueOf(streamPayment.amount().abs()))
      .successfulPayment(streamPayment.expectedAmount().equals(streamPayment.amount()))
      .senderDenomination(Denomination.builder()
        .assetCode(streamPayment.assetCode())
        .assetScale(streamPayment.assetScale())
        .build())
      .destinationDenomination(streamPayment.deliveredAssetCode().map(assetCode ->
        Denomination.builder()
          .assetCode(assetCode)
          .assetScale(streamPayment.deliveredAssetScale().get())
          .build()
      ))
      .amountLeftToSend(UnsignedLong.valueOf(streamPayment.expectedAmount().get().subtract(streamPayment.amount()).abs()))
      .senderAddress(streamPayment.sourceAddress())
      .numRejectPackets(0) // not tracked on StreamPayments
      .build();
  }
}
