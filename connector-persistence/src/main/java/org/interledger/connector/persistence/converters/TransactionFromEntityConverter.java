package org.interledger.connector.persistence.converters;

import org.interledger.connector.persistence.entities.TransactionEntity;
import org.interledger.connector.transactions.Transaction;
import org.interledger.connector.transactions.TransactionStatus;
import org.interledger.core.InterledgerAddress;

import com.google.common.base.Enums;
import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

public class TransactionFromEntityConverter implements Converter<TransactionEntity, Transaction> {

  @Override
  public Transaction convert(TransactionEntity source) {
    return Transaction.builder().accountId(source.getAccountId())
      .amount(UnsignedLong.valueOf(source.getAmount()))
      .assetCode(source.getAssetCode())
      .assetScale(source.getAssetScale())
      .createdAt(source.getCreatedDate())
      .destinationAddress(InterledgerAddress.of(source.getDestinationAddress()))
      .modifiedAt(source.getModifiedDate())
      .packetCount(source.getPacketCount())
      .referenceId(source.getReferenceId())
      .transactionStatus(Enums.getIfPresent(TransactionStatus.class, source.getStatus()).or(TransactionStatus.UNKNOWN))
      .build();
  }

}
