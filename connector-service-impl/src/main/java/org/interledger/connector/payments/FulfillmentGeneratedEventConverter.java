package org.interledger.connector.payments;

import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.core.InterledgerAddress;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import org.springframework.core.convert.converter.Converter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public class FulfillmentGeneratedEventConverter implements Converter<FulfillmentGeneratedEvent, StreamPayment> {

  private static final Set<StreamFrameType> CLOSING_FRAMES = Sets.newHashSet(
    StreamFrameType.ConnectionClose,
    StreamFrameType.StreamClose
  );

  public StreamPayment convert(FulfillmentGeneratedEvent event) {
    return StreamPayment.builder()
        .destinationAddress(event.incomingPreparePacket().getDestination())
        .sourceAddress(getSourceAddress(event.streamPacket()))
        .status(getTransactionStatus(event.streamPacket()))
        .packetCount(1)
        .streamPaymentId(hash(event.incomingPreparePacket().getDestination()))
        .modifiedAt(Instant.now())
        .createdAt(Instant.now())
        .assetScale(event.denomination().assetScale())
        .assetCode(event.denomination().assetCode())
        .amount(event.incomingPreparePacket().getAmount().bigIntegerValue())
        .accountId(event.accountId())
        .type(StreamPaymentType.PAYMENT_RECEIVED)
        .build();
  }

  private String hash(InterledgerAddress destinationAddress) {
    return Hashing.sha256()
      .hashString(destinationAddress.getValue(), StandardCharsets.US_ASCII)
      .toString();
  }

  private StreamPaymentStatus getTransactionStatus(StreamPacket streamPacket) {
    if (streamPacket.frames().stream()
      .map(StreamFrame::streamFrameType)
      .anyMatch(CLOSING_FRAMES::contains)) {
      return StreamPaymentStatus.CLOSED_BY_STREAM;
    } else {
      return StreamPaymentStatus.PENDING;
    }
  }

  private Optional<InterledgerAddress> getSourceAddress(StreamPacket streamPacket) {
    return streamPacket.frames().stream()
      .filter(frame -> frame.streamFrameType().equals(StreamFrameType.ConnectionNewAddress))
      .map(frame -> ((ConnectionNewAddressFrame) frame).sourceAddress())
      .findFirst();
  }

}
