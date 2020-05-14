package org.interledger.connector.payments;

import org.interledger.connector.persistence.entities.StreamPaymentEntity;
import org.interledger.core.InterledgerAddress;

import org.springframework.core.convert.converter.Converter;

public class StreamPaymentToEntityConverter implements Converter<StreamPayment, StreamPaymentEntity> {

  @Override
  public StreamPaymentEntity convert(StreamPayment source) {
    StreamPaymentEntity entity = new StreamPaymentEntity();
    entity.setAccountId(source.accountId());
    entity.setAmount(source.amount());
    entity.setExpectedAmount(source.expectedAmount().orElse(null));
    entity.setAssetScale(source.assetScale());
    entity.setAssetCode(source.assetCode());
    entity.setCreatedDate(source.createdAt());
    entity.setDestinationAddress(source.destinationAddress().getValue());
    entity.setModifiedDate(source.modifiedAt());
    entity.setPacketCount(source.packetCount());
    entity.setStreamPaymentId(source.streamPaymentId());
    entity.setSourceAddress(source.sourceAddress().map(InterledgerAddress::getValue).orElse(null));
    entity.setDeliveredAmount(source.deliveredAmount().bigIntegerValue());
    entity.setDeliveredAssetCode(source.deliveredAssetCode().orElse(null));
    entity.setDeliveredAssetScale(source.deliveredAssetScale().orElse(null));
    entity.setStatus(source.status());
    entity.setType(source.type());
    return entity;
  }

}
