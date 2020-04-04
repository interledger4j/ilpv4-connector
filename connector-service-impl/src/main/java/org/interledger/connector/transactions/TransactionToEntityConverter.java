package org.interledger.connector.transactions;

import org.interledger.connector.persistence.entities.TransactionEntity;

import org.springframework.core.convert.converter.Converter;

public class TransactionToEntityConverter implements Converter<Transaction, TransactionEntity> {

  @Override
  public TransactionEntity convert(Transaction source) {
    TransactionEntity entity = new TransactionEntity();
    entity.setAccountId(source.accountId());
    entity.setAmount(source.amount().bigIntegerValue());
    entity.setAssetScale(source.assetScale());
    entity.setAssetCode(source.assetCode());
    entity.setDestinationAddress(source.destinationAddress().getValue());
    entity.setPacketCount(source.packetCount());
    entity.setReferenceId(source.referenceId());
    entity.setStatus(source.transactionStatus().toString());
    return entity;
  }

}
