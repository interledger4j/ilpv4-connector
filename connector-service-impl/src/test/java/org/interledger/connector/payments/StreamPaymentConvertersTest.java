package org.interledger.connector.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.StreamPaymentEntity;
import org.interledger.core.InterledgerAddress;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public class StreamPaymentConvertersTest {

  private StreamPaymentFromEntityConverter fromEntityConverter = new StreamPaymentFromEntityConverter();

  private StreamPaymentToEntityConverter toEntityConverter = new StreamPaymentToEntityConverter();

  @Test
  public void convertBothWaysPaymentReceived() {
    InterledgerAddress sourceAddress = InterledgerAddress.of("test.source");
    InterledgerAddress destinationAddress = InterledgerAddress.of("test.dest");
    long amount = 100;
    long expectedAmount = 200;
    long deliveredAmount = 150;
    int packetCount = 12;
    StreamPaymentStatus streamPaymentStatus = StreamPaymentStatus.PENDING;
    StreamPaymentType streamPaymentType = StreamPaymentType.PAYMENT_RECEIVED;
    AccountId accountId = AccountId.of("bob");
    Instant createdAt = Instant.now().minusSeconds(10);
    Instant modifiedAt = Instant.now();
    String streamPaymentId = UUID.randomUUID().toString();
    String assetCode = "XRP";
    short assetScale = (short) 9;

    StreamPayment streamPayment = StreamPayment.builder()
      .accountId(accountId)
      .amount(BigInteger.valueOf(amount))
      .expectedAmount(BigInteger.valueOf(expectedAmount))
      .deliveredAmount(UnsignedLong.valueOf(deliveredAmount))
      .assetCode(assetCode)
      .assetScale(assetScale)
      .createdAt(createdAt)
      .destinationAddress(destinationAddress)
      .modifiedAt(modifiedAt)
      .packetCount(packetCount)
      .streamPaymentId(streamPaymentId)
      .sourceAddress(sourceAddress)
      .status(streamPaymentStatus)
      .type(streamPaymentType)
      .build();

    StreamPaymentEntity entity = new StreamPaymentEntity();
    entity.setAccountId(accountId);
    entity.setAmount(BigInteger.valueOf(amount));
    entity.setExpectedAmount(BigInteger.valueOf(expectedAmount));
    entity.setDeliveredAmount(BigInteger.valueOf(deliveredAmount));
    entity.setAssetCode(assetCode);
    entity.setAssetScale(assetScale);
    entity.setCreatedDate(createdAt);
    entity.setDestinationAddress(destinationAddress.getValue());
    entity.setModifiedDate(modifiedAt);
    entity.setPacketCount(packetCount);
    entity.setStreamPaymentId(streamPaymentId);
    entity.setSourceAddress(sourceAddress.getValue());
    entity.setStatus(streamPaymentStatus);
    entity.setType(streamPaymentType);

    assertThat(toEntityConverter.convert(streamPayment)).isEqualTo(entity);
    assertThat(fromEntityConverter.convert(entity)).isEqualTo(streamPayment);
  }

  @Test
  public void convertBothWaysPaymentSent() {
    InterledgerAddress sourceAddress = InterledgerAddress.of("test.source");
    InterledgerAddress destinationAddress = InterledgerAddress.of("test.dest");
    long amount = -100;
    long expectedAmount = -200;
    long deliveredAmount = 150;
    int packetCount = 12;
    StreamPaymentStatus streamPaymentStatus = StreamPaymentStatus.PENDING;
    StreamPaymentType streamPaymentType = StreamPaymentType.PAYMENT_SENT;
    AccountId accountId = AccountId.of("bob");
    Instant createdAt = Instant.now().minusSeconds(10);
    Instant modifiedAt = Instant.now();
    String streamPaymentId = UUID.randomUUID().toString();
    String assetCode = "XRP";
    short assetScale = (short) 9;

    StreamPayment streamPayment = StreamPayment.builder()
      .accountId(accountId)
      .amount(BigInteger.valueOf(amount))
      .expectedAmount(BigInteger.valueOf(expectedAmount))
      .deliveredAmount(UnsignedLong.valueOf(deliveredAmount))
      .assetCode(assetCode)
      .assetScale(assetScale)
      .createdAt(createdAt)
      .destinationAddress(destinationAddress)
      .modifiedAt(modifiedAt)
      .packetCount(packetCount)
      .streamPaymentId(streamPaymentId)
      .sourceAddress(sourceAddress)
      .status(streamPaymentStatus)
      .type(streamPaymentType)
      .build();

    StreamPaymentEntity entity = new StreamPaymentEntity();
    entity.setAccountId(accountId);
    entity.setAmount(BigInteger.valueOf(amount));
    entity.setExpectedAmount(BigInteger.valueOf(expectedAmount));
    entity.setDeliveredAmount(BigInteger.valueOf(deliveredAmount));
    entity.setAssetCode(assetCode);
    entity.setAssetScale(assetScale);
    entity.setCreatedDate(createdAt);
    entity.setDestinationAddress(destinationAddress.getValue());
    entity.setModifiedDate(modifiedAt);
    entity.setPacketCount(packetCount);
    entity.setStreamPaymentId(streamPaymentId);
    entity.setSourceAddress(sourceAddress.getValue());
    entity.setStatus(streamPaymentStatus);
    entity.setType(streamPaymentType);

    assertThat(toEntityConverter.convert(streamPayment)).isEqualTo(entity);
    assertThat(fromEntityConverter.convert(entity)).isEqualTo(streamPayment);
  }
}
