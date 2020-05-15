package org.interledger.connector.payments;

import org.interledger.connector.persistence.entities.StreamPaymentEntity;
import org.interledger.core.InterledgerAddress;

import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

public class StreamPaymentFromEntityConverter implements Converter<StreamPaymentEntity, StreamPayment> {

  @Override
  public StreamPayment convert(StreamPaymentEntity source) {
    return StreamPayment.builder()
      .accountId(source.getAccountId())
      .amount(source.getAmount())
      .expectedAmount(Optional.ofNullable(source.getExpectedAmount()))
      .assetCode(source.getAssetCode())
      .assetScale(source.getAssetScale())
      .correlationId(Optional.ofNullable(source.getCorrelationId()))
      .createdAt(source.getCreatedDate())
      .destinationAddress(InterledgerAddress.of(source.getDestinationAddress()))
      .modifiedAt(source.getModifiedDate())
      .packetCount(source.getPacketCount())
      .streamPaymentId(source.getStreamPaymentId())
      .sourceAddress(Optional.ofNullable(source.getSourceAddress()).map(InterledgerAddress::of))
      .deliveredAmount(UnsignedLong.valueOf(source.getDeliveredAmount()))
      .deliveredAssetCode(Optional.ofNullable(source.getDeliveredAssetCode()))
      .deliveredAssetScale(Optional.ofNullable(source.getDeliveredAssetScale()))
      .status(source.getStatus())
      .type(source.getType())
      .build();
  }

}
