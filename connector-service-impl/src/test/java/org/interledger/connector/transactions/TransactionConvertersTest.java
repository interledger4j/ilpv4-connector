package org.interledger.connector.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.TransactionEntity;
import org.interledger.core.InterledgerAddress;

import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public class TransactionConvertersTest {

  private TransactionFromEntityConverter fromEntityConverter = new TransactionFromEntityConverter();

  private TransactionToEntityConverter toEntityConverter = new TransactionToEntityConverter();

  @Test
  public void convertBothWays() {
    InterledgerAddress sourceAddress = InterledgerAddress.of("test.source");
    InterledgerAddress destinationAddress = InterledgerAddress.of("test.dest");
    long amount = 100;
    int packetCount = 12;
    TransactionStatus transactionStatus = TransactionStatus.PENDING;
    TransactionType transactionType = TransactionType.PAYMENT_RECEIVED;
    AccountId accountId = AccountId.of("bob");
    Instant createdAt = Instant.now().minusSeconds(10);
    Instant modifiedAt = Instant.now();
    String transactionId = UUID.randomUUID().toString();
    String assetCode = "XRP";
    short assetScale = (short) 9;

    Transaction transaction = Transaction.builder()
      .accountId(accountId)
      .amount(BigInteger.valueOf(amount))
      .assetCode(assetCode)
      .assetScale(assetScale)
      .createdAt(createdAt)
      .destinationAddress(destinationAddress)
      .modifiedAt(modifiedAt)
      .packetCount(packetCount)
      .transactionId(transactionId)
      .sourceAddress(sourceAddress)
      .status(transactionStatus)
      .type(transactionType)
      .build();

    TransactionEntity entity = new TransactionEntity();
    entity.setAccountId(accountId);
    entity.setAmount(BigInteger.valueOf(amount));
    entity.setAssetCode(assetCode);
    entity.setAssetScale(assetScale);
    entity.setCreatedDate(createdAt);
    entity.setDestinationAddress(destinationAddress.getValue());
    entity.setModifiedDate(modifiedAt);
    entity.setPacketCount(packetCount);
    entity.setTransactionId(transactionId);
    entity.setSourceAddress(sourceAddress.getValue());
    entity.setStatus(transactionStatus);
    entity.setType(transactionType);

    assertThat(toEntityConverter.convert(transaction)).isEqualTo(entity);
    assertThat(fromEntityConverter.convert(entity)).isEqualTo(transaction);
  }
}