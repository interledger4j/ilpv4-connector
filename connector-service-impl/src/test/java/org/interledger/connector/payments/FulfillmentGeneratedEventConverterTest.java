package org.interledger.connector.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;

public class FulfillmentGeneratedEventConverterTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("bob");
  private static final Denomination DENOMINATION = Denomination.builder()
    .assetScale((short) 9)
    .assetCode("XRP")
    .build();
  public static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("g.destination");

  private StreamEncryptionService streamEncryptionService;
  private CodecContext streamCodecContext
    ;
  private FulfillmentGeneratedEventConverter converter = new FulfillmentGeneratedEventConverter(
    streamEncryptionService, streamCodecContext);

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
      .incomingPreparePacket(preparePacket(amount, streamPacket))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .paymentType(StreamPaymentType.PAYMENT_RECEIVED)
      .fulfillPacket(InterledgerFulfillPacket.builder()
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .data(new byte[0])
        .build()
      )
      .build();

    StreamPayment streamPayment = converter.convert(event);
    StreamPayment expected = StreamPayment.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(StreamPaymentStatus.PENDING)
      .accountId(ACCOUNT_ID)
      .amount(BigInteger.valueOf(amount))
      .packetCount(1)
      .deliveredAmount(UnsignedLong.valueOf(amount))
      .deliveredAssetScale(DENOMINATION.assetScale())
      .deliveredAssetCode(DENOMINATION.assetCode())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(streamPayment.createdAt())
      .modifiedAt(streamPayment.modifiedAt())
      .type(StreamPaymentType.PAYMENT_RECEIVED)
      .build();

    assertThat(streamPayment).isEqualTo(expected);
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
      .incomingPreparePacket(preparePacket(amount, streamPacket))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .paymentType(StreamPaymentType.PAYMENT_RECEIVED)
      .fulfillPacket(InterledgerFulfillPacket.builder()
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .data(new byte[0])
        .build()
      )
      .build();

    StreamPayment streamPayment = converter.convert(event);
    StreamPayment expected = StreamPayment.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(StreamPaymentStatus.CLOSED_BY_STREAM)
      .accountId(ACCOUNT_ID)
      .amount(BigInteger.valueOf(amount))
      .packetCount(1)
      .deliveredAmount(UnsignedLong.valueOf(amount))
      .deliveredAssetScale(DENOMINATION.assetScale())
      .deliveredAssetCode(DENOMINATION.assetCode())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(streamPayment.createdAt())
      .modifiedAt(streamPayment.modifiedAt())
      .type(StreamPaymentType.PAYMENT_RECEIVED)
      .build();

    assertThat(streamPayment).isEqualTo(expected);
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
      .incomingPreparePacket(preparePacket(amount, streamPacket))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .paymentType(StreamPaymentType.PAYMENT_RECEIVED)
      .fulfillPacket(InterledgerFulfillPacket.builder()
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .data(new byte[0])
        .build()
      )
      .build();

    StreamPayment streamPayment = converter.convert(event);
    StreamPayment expected = StreamPayment.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(StreamPaymentStatus.PENDING)
      .accountId(ACCOUNT_ID)
      .amount(BigInteger.valueOf(amount))
      .packetCount(1)
      .deliveredAmount(UnsignedLong.valueOf(amount))
      .deliveredAssetScale(DENOMINATION.assetScale())
      .deliveredAssetCode(DENOMINATION.assetCode())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(streamPayment.createdAt())
      .modifiedAt(streamPayment.modifiedAt())
      .sourceAddress(source)
      .type(StreamPaymentType.PAYMENT_RECEIVED)
      .build();

    assertThat(streamPayment).isEqualTo(expected);
  }

  private InterledgerPreparePacket.AbstractInterledgerPreparePacket preparePacket(long amount,
                                                                                  StreamPacket streamPacket) {
    return InterledgerPreparePacket.builder()
      .amount(UnsignedLong.valueOf(amount))
      .expiresAt(Instant.now())
      .destination(DESTINATION_ADDRESS)
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .typedData(streamPacket)
      .build();
  }

  private StreamMoneyFrame moneyFrame() {
    return StreamMoneyFrame.builder().shares(UnsignedLong.ONE)
      .streamId(UnsignedLong.ONE)
      .build();
  }

  private StreamCloseFrame closeFrame() {
    return StreamCloseFrame.builder()
      .streamId(UnsignedLong.ONE)
      .build();
  }

}