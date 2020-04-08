package org.interledger.connector.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class FulfillmentGeneratedEventConverterTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("bob");
  private static final Denomination DENOMINATION = Denomination.builder()
    .assetScale((short) 9)
    .assetCode("XRP")
    .build();
  public static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("g.destination");

  private FulfillmentGeneratedEventConverter converter = new FulfillmentGeneratedEventConverter();

  @Test
  public void convert() {
    long amount = 100;
    StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .addFrames(moneyFrame())
      .build();
    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .streamPacket(streamPacket)
      .incomingPreparePacket(preparePacket(amount))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();

    Transaction transaction = converter.convert(event);
    Transaction expected = Transaction.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(TransactionStatus.PENDING)
      .accountId(ACCOUNT_ID)
      .amount(BigInteger.valueOf(amount))
      .packetCount(1)
      .transactionId(Hashing.sha256().hashString(DESTINATION_ADDRESS.getValue(), StandardCharsets.UTF_8).toString())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(transaction.createdAt())
      .modifiedAt(transaction.modifiedAt())
      .type(TransactionType.PAYMENT_RECEIVED)
      .build();

    assertThat(transaction).isEqualTo(expected);
  }

  @Test
  public void convertWithClosingFrame() {
    long amount = 100;
    StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .addFrames(moneyFrame())
      .addFrames(StreamCloseFrame.builder().errorCode(ErrorCodes.NoError).streamId(UnsignedLong.ONE).build())
      .build();
    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .streamPacket(streamPacket)
      .incomingPreparePacket(preparePacket(amount))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();

    Transaction transaction = converter.convert(event);
    Transaction expected = Transaction.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(TransactionStatus.CLOSED_BY_STREAM)
      .accountId(ACCOUNT_ID)
      .amount(BigInteger.valueOf(amount))
      .packetCount(1)
      .transactionId(Hashing.sha256().hashString(DESTINATION_ADDRESS.getValue(), StandardCharsets.UTF_8).toString())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(transaction.createdAt())
      .modifiedAt(transaction.modifiedAt())
      .type(TransactionType.PAYMENT_RECEIVED)
      .build();

    assertThat(transaction).isEqualTo(expected);
  }

  @Test
  public void convertWithConnectionNewAddressFrame() {
    long amount = 100;
    InterledgerAddress source = InterledgerAddress.of("test.source");
    StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .addFrames(moneyFrame())
      .addFrames(ConnectionNewAddressFrame.builder().sourceAddress(source).build())
      .build();
    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .streamPacket(streamPacket)
      .incomingPreparePacket(preparePacket(amount))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();

    Transaction transaction = converter.convert(event);
    Transaction expected = Transaction.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(TransactionStatus.PENDING)
      .accountId(ACCOUNT_ID)
      .amount(BigInteger.valueOf(amount))
      .packetCount(1)
      .transactionId(Hashing.sha256().hashString(DESTINATION_ADDRESS.getValue(), StandardCharsets.UTF_8).toString())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(transaction.createdAt())
      .modifiedAt(transaction.modifiedAt())
      .sourceAddress(source)
      .type(TransactionType.PAYMENT_RECEIVED)
      .build();

    assertThat(transaction).isEqualTo(expected);
  }

  private InterledgerPreparePacket.AbstractInterledgerPreparePacket preparePacket(long amount) {
    return InterledgerPreparePacket.builder()
      .amount(UnsignedLong.valueOf(amount))
      .expiresAt(Instant.now())
      .destination(DESTINATION_ADDRESS)
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .build();
  }

  private StreamMoneyFrame moneyFrame() {
    return StreamMoneyFrame.builder().shares(UnsignedLong.ONE)
      .streamId(UnsignedLong.ONE)
      .build();
  }

}