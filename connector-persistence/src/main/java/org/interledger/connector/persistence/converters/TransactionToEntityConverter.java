package org.interledger.connector.persistence.converters;

import org.interledger.connector.persistence.entities.TransactionEntity;
import org.interledger.connector.transactions.Transaction;
import org.interledger.core.InterledgerAddress;

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
    entity.setSourceAddress(source.sourceAddress().map(InterledgerAddress::getValue).orElse(null));
    entity.setStatus(source.status().toString());
    entity.setType(source.type().toString());
    return entity;
  }

}
