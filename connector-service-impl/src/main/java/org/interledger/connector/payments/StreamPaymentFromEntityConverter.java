package org.interledger.connector.payments;

import org.interledger.connector.persistence.entities.StreamPaymentEntity;
import org.interledger.core.InterledgerAddress;

import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

public class StreamPaymentFromEntityConverter implements Converter<StreamPaymentEntity, StreamPayment> {

  @Override
  public StreamPayment convert(StreamPaymentEntity source) {
    return StreamPayment.builder()
      .accountId(source.getAccountId())
      .amount(source.getAmount())
      .assetCode(source.getAssetCode())
      .assetScale(source.getAssetScale())
      .createdAt(source.getCreatedDate())
      .destinationAddress(InterledgerAddress.of(source.getDestinationAddress()))
      .modifiedAt(source.getModifiedDate())
      .packetCount(source.getPacketCount())
      .streamPaymentId(source.getStreamPaymentId())
      .sourceAddress(Optional.ofNullable(source.getSourceAddress()).map(InterledgerAddress::of))
      .status(source.getStatus())
      .type(source.getType())
      .build();
  }

}
