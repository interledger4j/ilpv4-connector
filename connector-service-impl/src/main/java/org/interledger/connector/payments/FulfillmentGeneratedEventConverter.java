package org.interledger.connector.payments;

import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.connector.localsend.StreamPacketWithSharedSecret;
import org.interledger.connector.stream.StreamPacketUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.sender.StreamSenderException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class FulfillmentGeneratedEventConverter implements Converter<FulfillmentGeneratedEvent, StreamPayment> {

  private final StreamEncryptionService streamEncryptionService;
  private final CodecContext streamCodecContext;

  public FulfillmentGeneratedEventConverter(StreamEncryptionService streamEncryptionService, CodecContext streamCodecContext) {
    this.streamEncryptionService = streamEncryptionService;
    this.streamCodecContext = streamCodecContext;
  }

  @Override
  public StreamPayment convert(FulfillmentGeneratedEvent source) {
    switch (source.paymentType()) {
      case PAYMENT_RECEIVED: return convertToPaymentReceived(source);
      case PAYMENT_SENT: return convertToPaymentSent(source);
      default: throw new IllegalArgumentException("found unmapped paymentType " + source.paymentType() +
        " for event: " + source);
    }
  }

  public StreamPayment convertToPaymentReceived(FulfillmentGeneratedEvent event) {
    ImmutableStreamPayment.Builder builder = newBuilder(event);

    builder.amount(event.incomingPreparePacket().getAmount().bigIntegerValue())
      .deliveredAmount(event.incomingPreparePacket().getAmount())
      .deliveredAssetScale(event.denomination().assetScale())
      .deliveredAssetCode(event.denomination().assetCode())
      .type(StreamPaymentType.PAYMENT_RECEIVED);
    return builder.build();
  }

  public StreamPayment convertToPaymentSent(FulfillmentGeneratedEvent event) {
    Optional<StreamPacket> responseStreamPacket = streamPacket(event.fulfillPacket());
    Optional<Denomination> deliveredDenomination = responseStreamPacket.flatMap(StreamPacketUtils::getDenomination);
    ImmutableStreamPayment.Builder builder = newBuilder(event);

    builder.amount(event.incomingPreparePacket().getAmount().bigIntegerValue().negate())
      .deliveredAmount(responseStreamPacket.map(StreamPacket::prepareAmount).orElse(UnsignedLong.ZERO))
      .deliveredAssetScale(deliveredDenomination.map(Denomination::assetScale))
      .deliveredAssetCode(deliveredDenomination.map(Denomination::assetCode))
      .type(StreamPaymentType.PAYMENT_SENT);
    return builder.build();
  }

  private ImmutableStreamPayment.Builder newBuilder(FulfillmentGeneratedEvent event) {
    Optional<StreamPacket> prepareStreamPacket = streamPacket(event.incomingPreparePacket());
    Optional<InterledgerAddress> sourceAddress = prepareStreamPacket.flatMap(StreamPacketUtils::getSourceAddress);
    return StreamPayment.builder()
      .destinationAddress(event.incomingPreparePacket().getDestination())
      .sourceAddress(sourceAddress)
      .status(getTransactionStatus(prepareStreamPacket))
      .packetCount(1)
      .modifiedAt(Instant.now())
      .createdAt(Instant.now())
      .assetScale(event.denomination().assetScale())
      .assetCode(event.denomination().assetCode())
      .accountId(event.accountId());
  }

  private StreamPaymentStatus getTransactionStatus(Optional<StreamPacket> maybeStreamPacket) {
    return maybeStreamPacket.map(streamPacket -> {
      if (StreamPacketUtils.hasCloseFrame(streamPacket)) {
        return StreamPaymentStatus.CLOSED_BY_STREAM;
      } else {
        return StreamPaymentStatus.PENDING;
      }
    }).orElse(StreamPaymentStatus.PENDING);
  }

  /**
   * Convert the encrypted bytes of a stream packet into a {@link StreamPacket} using the CodecContext and {@code
   * sharedSecret}.
   *
   * @param sharedSecret               The shared secret known only to this client and the remote STREAM receiver,
   *                                   used to encrypt and decrypt STREAM frames and packets sent and received inside
   *                                   of ILPv4 packets sent over the Interledger between these two entities (i.e.,
   *                                   sender and receiver).
   * @param encryptedStreamPacketBytes A byte-array containing an encrypted ASN.1 OER encoded {@link StreamPacket}.
   * @return The decrypted {@link StreamPacket}.
   */
  @VisibleForTesting
  StreamPacket fromEncrypted(final SharedSecret sharedSecret, final byte[] encryptedStreamPacketBytes) {
    Objects.requireNonNull(encryptedStreamPacketBytes);

    final byte[] streamPacketBytes = this.streamEncryptionService.decrypt(sharedSecret, encryptedStreamPacketBytes);
    try {
      return streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
    } catch (IOException e) {
      throw new StreamSenderException(e.getMessage(), e);
    }
  }

  private Optional<StreamPacket> streamPacket(InterledgerPacket interledgerPacket) {
    return interledgerPacket.typedData().flatMap(typedData -> {
      if (typedData instanceof StreamPacketWithSharedSecret) {
        return Optional.of(
          fromEncrypted(((StreamPacketWithSharedSecret) typedData).sharedSecret(), interledgerPacket.getData()));
      }
      else if (typedData instanceof StreamPacket) {
        return Optional.of((StreamPacket) typedData);
      } else {
        return Optional.empty();
      }
    });
  }

}
