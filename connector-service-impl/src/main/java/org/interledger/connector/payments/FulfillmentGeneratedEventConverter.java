package org.interledger.connector.payments;

import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.connector.stream.StreamPacketUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.sender.StreamSenderException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
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

  public FulfillmentGeneratedEventConverter(StreamEncryptionService streamEncryptionService,
    CodecContext streamCodecContext) {
    this.streamEncryptionService = streamEncryptionService;
    this.streamCodecContext = streamCodecContext;
  }

  @Override
  public StreamPayment convert(FulfillmentGeneratedEvent source) {
    Optional<StreamPacket> prepareStreamPacket =
      streamPreparePacket(source.preparePacket(), source.fulfillPacket());

    Optional<StreamPacket> fulfillStreamPacket =
      streamFulfillPacket(source.preparePacket(), source.fulfillPacket());

    Optional<InterledgerAddress> sourceAddress = prepareStreamPacket.flatMap(StreamPacketUtils::getSourceAddress);
    ImmutableStreamPayment.Builder builder = StreamPayment.builder()
      .destinationAddress(source.preparePacket().getDestination())
      .sourceAddress(sourceAddress)
      .status(getTransactionStatus(prepareStreamPacket).orElseGet(
        () -> getTransactionStatus(fulfillStreamPacket).orElse(StreamPaymentStatus.PENDING)
      ))
      .packetCount(1)
      .modifiedAt(Instant.now())
      .createdAt(Instant.now())
      .assetScale(source.denomination().assetScale())
      .assetCode(source.denomination().assetCode())
      .accountId(source.accountId());

    switch (source.paymentType()) {
      case PAYMENT_RECEIVED: {
        return builder.amount(source.preparePacket().getAmount().bigIntegerValue())
          .deliveredAmount(source.preparePacket().getAmount())
          .deliveredAssetScale(source.denomination().assetScale())
          .deliveredAssetCode(source.denomination().assetCode())
          .type(StreamPaymentType.PAYMENT_RECEIVED)
          .build();
      }
      case PAYMENT_SENT: {
        Optional<Denomination> deliveredDenomination = fulfillStreamPacket.flatMap(StreamPacketUtils::getDenomination);
        return builder.amount(source.preparePacket().getAmount().bigIntegerValue().negate())
          .deliveredAmount(fulfillStreamPacket.map(StreamPacket::prepareAmount).orElse(UnsignedLong.ZERO))
          .deliveredAssetScale(deliveredDenomination.map(Denomination::assetScale))
          .deliveredAssetCode(deliveredDenomination.map(Denomination::assetCode))
          .type(StreamPaymentType.PAYMENT_SENT)
          .build();
      }
      default:
        throw new IllegalArgumentException("found unmapped paymentType " + source.paymentType() +
          " for event: " + source);
    }
  }

  private Optional<StreamPaymentStatus> getTransactionStatus(Optional<StreamPacket> maybeStreamPacket) {
    return maybeStreamPacket.flatMap(streamPacket -> {
      if (StreamPacketUtils.hasCloseFrame(streamPacket)) {
        return Optional.of(StreamPaymentStatus.CLOSED_BY_STREAM);
      } else {
        return Optional.empty();
      }
    });
  }

  /**
   * Convert the encrypted bytes of a stream packet into a {@link StreamPacket} using the CodecContext and {@code
   * sharedSecret}.
   *
   * @param sharedSecret               The shared secret known only to this client and the remote STREAM receiver, used
   *                                   to encrypt and decrypt STREAM frames and packets sent and received inside of
   *                                   ILPv4 packets sent over the Interledger between these two entities (i.e., sender
   *                                   and receiver).
   * @param encryptedStreamPacketBytes A byte-array containing an encrypted ASN.1 OER encoded {@link StreamPacket}.
   *
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

  private Optional<StreamPacket> streamPreparePacket(InterledgerPreparePacket preparePacket,
    InterledgerFulfillPacket fulfillPacket) {
    return findStreamPacket(preparePacket, fulfillPacket);
  }

  private Optional<StreamPacket> streamFulfillPacket(InterledgerPreparePacket preparePacket,
    InterledgerFulfillPacket fulfillPacket) {
    return findStreamPacket(fulfillPacket, preparePacket);
  }


  private Optional<StreamPacket> findStreamPacket(InterledgerPacket primary,
    InterledgerPacket secondary) {
    Optional<StreamPacket> fromTypedData = primary.typedData().flatMap(typedData -> {
      if (typedData instanceof StreamPacket) {
        return Optional.of((StreamPacket) typedData);
      } else {
        return Optional.empty();
      }
    });
    if (fromTypedData.isPresent()) {
      return fromTypedData;
    } else {
      return getSharedSecret(primary, secondary)
        .map(sharedSecret -> fromEncrypted(sharedSecret, primary.getData()));
    }
  }

  /**
   * Helper method to obtain an optionally present {@link SharedSecret} from an array of STREAM packets.
   *
   * @param packets An array of type {@link StreamPacket}.
   *
   * @return The first {@link SharedSecret} encountered.
   */
  @VisibleForTesting
  protected Optional<SharedSecret> getSharedSecret(InterledgerPacket... packets) {
    return Lists.newArrayList(packets).stream()
      .map(InterledgerPacket::typedData)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(typedData -> typedData instanceof StreamPacket)
      .map(typedData -> (StreamPacket) typedData)
      .map(StreamPacket::sharedSecret)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

}
