package org.interledger.connector.transactions;

import org.interledger.connector.persistence.entities.TransactionEntity;
import org.interledger.core.InterledgerAddress;

import org.springframework.core.convert.converter.Converter;

public class TransactionToEntityConverter implements Converter<Transaction, TransactionEntity> {

  @Override
  public TransactionEntity convert(Transaction source) {
    TransactionEntity entity = new TransactionEntity();
    entity.setAccountId(source.accountId());
    entity.setAmount(source.amount());
    entity.setAssetScale(source.assetScale());
    entity.setAssetCode(source.assetCode());
    entity.setCreatedDate(source.createdAt());
    entity.setDestinationAddress(source.destinationAddress().getValue());
    entity.setModifiedDate(source.modifiedAt());
    entity.setPacketCount(source.packetCount());
    entity.setTransactionId(source.transactionId());
    entity.setSourceAddress(source.sourceAddress().map(InterledgerAddress::getValue).orElse(null));
    entity.setStatus(source.status());
    entity.setType(source.type());
    return entity;
  }

}
