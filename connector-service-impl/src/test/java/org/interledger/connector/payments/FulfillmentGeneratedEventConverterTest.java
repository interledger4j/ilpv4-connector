package org.interledger.connector.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.SharedSecret;
import org.interledger.crypto.ByteArrayUtils;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.AesGcmStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.frames.StreamMoneyMaxFrame;
import org.interledger.stream.sender.StreamSenderException;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class FulfillmentGeneratedEventConverterTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("bob");
  private static final Denomination DENOMINATION = Denomination.builder()
    .assetScale((short) 9)
    .assetCode("XRP")
    .build();
  public static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("g.destination");

  private StreamEncryptionService streamEncryptionService;
  private CodecContext streamCodecContext;

  private FulfillmentGeneratedEventConverter converter;

  @Before
  public void setUp() {
    streamEncryptionService = new AesGcmStreamEncryptionService();
    streamCodecContext = StreamCodecContextFactory.oer();
    converter = new FulfillmentGeneratedEventConverter(streamEncryptionService, streamCodecContext);
  }

  @Test
  public void convertPaymentReceived() {
    long amount = 100;
    StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .addFrames(moneyFrame())
      .build();
    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .preparePacket(preparePacket(amount, streamPacket))
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
  public void convertPaymentReceivedWithoutTypedData() {
    long amount = 100;
    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .preparePacket(preparePacket(amount, Optional.empty()))
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
  public void convertWithClosingFrameOnPrepare() {
    long amount = 100;
    StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .addFrames(moneyFrame())
      .addFrames(closeFrame())
      .build();
    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .preparePacket(preparePacket(amount, streamPacket))
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
  public void convertWithClosingFrameOnFulfill() {
    long amount = 100;
    StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .addFrames(moneyFrame())
      .build();

    StreamPacket streamResponsePacket = StreamPacket.builder()
      .sequence(UnsignedLong.ONE)
      .interledgerPacketType(InterledgerPacketType.FULFILL)
      .prepareAmount(UnsignedLong.valueOf(amount))
      .addFrames(moneyMaxFrame(), assetDetailsFrame(DENOMINATION))
      .addFrames(closeFrame())
      .build();

    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .preparePacket(preparePacket(amount, streamPacket))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .paymentType(StreamPaymentType.PAYMENT_SENT)
      .fulfillPacket(InterledgerFulfillPacket.builder()
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .typedData(streamResponsePacket)
        .build()
      )
      .build();

    StreamPayment streamPayment = converter.convert(event);
    StreamPayment expected = StreamPayment.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(StreamPaymentStatus.CLOSED_BY_STREAM)
      .accountId(ACCOUNT_ID)
      .amount(BigInteger.valueOf(amount).negate())
      .packetCount(1)
      .deliveredAmount(UnsignedLong.valueOf(amount))
      .deliveredAssetScale(DENOMINATION.assetScale())
      .deliveredAssetCode(DENOMINATION.assetCode())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(streamPayment.createdAt())
      .modifiedAt(streamPayment.modifiedAt())
      .type(StreamPaymentType.PAYMENT_SENT)
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
      .preparePacket(preparePacket(amount, streamPacket))
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

  @Test
  public void convertPaymentSentWithSharedSecret() {
    SharedSecret sharedSecret = SharedSecret.of(ByteArrayUtils.generate32RandomBytes());
    UnsignedLong sentAmount = UnsignedLong.valueOf(20);
    UnsignedLong deliveredAmount = UnsignedLong.valueOf(15);

    StreamPacket streamPreparePacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .sharedSecret(sharedSecret)
      .addFrames(moneyFrame())
      .build();

    StreamPacket streamResponsePacket = StreamPacket.builder()
      .sequence(UnsignedLong.ONE)
      .interledgerPacketType(InterledgerPacketType.FULFILL)
      .prepareAmount(deliveredAmount)
      .addFrames(moneyMaxFrame(), assetDetailsFrame(DENOMINATION))
      .build();

    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .preparePacket(preparePacket(sentAmount.longValue(), streamPreparePacket))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .paymentType(StreamPaymentType.PAYMENT_SENT)
      .fulfillPacket(InterledgerFulfillPacket.builder()
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .data(toEncrypted(sharedSecret, streamResponsePacket))
        .build()
      )
      .build();

    StreamPayment streamPayment = converter.convert(event);
    StreamPayment expected = StreamPayment.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(StreamPaymentStatus.PENDING)
      .accountId(ACCOUNT_ID)
      .amount(sentAmount.bigIntegerValue().negate())
      .packetCount(1)
      .deliveredAmount(deliveredAmount)
      .deliveredAssetScale(DENOMINATION.assetScale())
      .deliveredAssetCode(DENOMINATION.assetCode())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(streamPayment.createdAt())
      .modifiedAt(streamPayment.modifiedAt())
      .type(StreamPaymentType.PAYMENT_SENT)
      .build();

    assertThat(streamPayment).isEqualTo(expected);
  }

  @Test
  public void convertPaymentSentWithTypedData() {
    SharedSecret sharedSecret = SharedSecret.of(ByteArrayUtils.generate32RandomBytes());
    UnsignedLong sentAmount = UnsignedLong.valueOf(20);
    UnsignedLong deliveredAmount = UnsignedLong.valueOf(20);

    StreamPacket streamPreparePacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .sequence(UnsignedLong.ONE)
      .prepareAmount(UnsignedLong.valueOf(10))
      .sharedSecret(sharedSecret)
      .addFrames(moneyFrame())
      .build();

    StreamPacket streamResponsePacket = StreamPacket.builder()
      .sequence(UnsignedLong.ONE)
      .interledgerPacketType(InterledgerPacketType.FULFILL)
      .prepareAmount(deliveredAmount)
      .addFrames(moneyMaxFrame(), assetDetailsFrame(DENOMINATION))
      .build();

    FulfillmentGeneratedEvent event = FulfillmentGeneratedEvent.builder()
      .preparePacket(preparePacket(sentAmount.longValue(), streamPreparePacket))
      .accountId(ACCOUNT_ID)
      .denomination(DENOMINATION)
      .paymentType(StreamPaymentType.PAYMENT_SENT)
      .fulfillPacket(InterledgerFulfillPacket.builder()
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .typedData(streamResponsePacket)
        .build()
      )
      .build();

    StreamPayment streamPayment = converter.convert(event);
    StreamPayment expected = StreamPayment.builder()
      .assetScale(DENOMINATION.assetScale())
      .assetCode(DENOMINATION.assetCode())
      .status(StreamPaymentStatus.PENDING)
      .accountId(ACCOUNT_ID)
      .amount(sentAmount.bigIntegerValue().negate())
      .packetCount(1)
      .deliveredAmount(deliveredAmount)
      .deliveredAssetScale(DENOMINATION.assetScale())
      .deliveredAssetCode(DENOMINATION.assetCode())
      .destinationAddress(DESTINATION_ADDRESS)
      .createdAt(streamPayment.createdAt())
      .modifiedAt(streamPayment.modifiedAt())
      .type(StreamPaymentType.PAYMENT_SENT)
      .build();

    assertThat(streamPayment).isEqualTo(expected);
  }

  private InterledgerPreparePacket.AbstractInterledgerPreparePacket preparePacket(long amount,
                                                                                  Optional<StreamPacket> streamPayment)
  {
    return InterledgerPreparePacket.builder()
      .amount(UnsignedLong.valueOf(amount))
      .expiresAt(Instant.now())
      .destination(DESTINATION_ADDRESS)
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .typedData(streamPayment)
      .build();
  }

  private InterledgerPreparePacket.AbstractInterledgerPreparePacket preparePacket(long amount,
                                                                                  StreamPacket streamPacket) {
    return preparePacket(amount, Optional.of(streamPacket));
  }

  private StreamMoneyFrame moneyFrame() {
    return StreamMoneyFrame.builder().shares(UnsignedLong.ONE)
      .streamId(UnsignedLong.ONE)
      .build();
  }

  private ConnectionAssetDetailsFrame assetDetailsFrame(Denomination denomination) {
    return ConnectionAssetDetailsFrame.builder()
      .sourceDenomination(denomination)
      .build();
  }

  private StreamMoneyMaxFrame moneyMaxFrame() {
    return StreamMoneyMaxFrame.builder()
      .streamId(UnsignedLong.ONE)
      .totalReceived(UnsignedLong.ZERO)
      .receiveMax(UnsignedLong.MAX_VALUE)
      .build();
  }

  private StreamCloseFrame closeFrame() {
    return StreamCloseFrame.builder()
      .streamId(UnsignedLong.ONE)
      .errorCode(ErrorCodes.NoError)
      .build();
  }

  byte[] toEncrypted(final SharedSecret sharedSecret, final StreamPacket streamPacket) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(streamPacket);

    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCodecContext.write(streamPacket, baos);
      final byte[] streamPacketBytes = baos.toByteArray();
      return streamEncryptionService.encrypt(sharedSecret, streamPacketBytes);
    } catch (IOException e) {
      throw new StreamSenderException(e.getMessage(), e);
    }
  }

}