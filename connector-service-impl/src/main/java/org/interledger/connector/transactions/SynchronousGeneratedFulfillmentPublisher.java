package org.interledger.connector.transactions;

import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.time.Instant;
import java.util.Set;

public class SynchronousGeneratedFulfillmentPublisher implements GeneratedFulfillmentPublisher {

  private static final Set<StreamFrameType> CLOSING_FRAMES = Sets.newHashSet(
    StreamFrameType.ConnectionClose,
    StreamFrameType.StreamClose
  );

  private final PaymentTransactionAggregator paymentTransactionAggregator;

  public SynchronousGeneratedFulfillmentPublisher(PaymentTransactionAggregator paymentTransactionAggregator,
                                                  EventBus eventBus) {
    this.paymentTransactionAggregator = paymentTransactionAggregator;
    eventBus.register(this);
  }

  @Override
  @Subscribe
  public void receive(FulfillmentGeneratedEvent event) {
    paymentTransactionAggregator.aggregate(
      Transaction.builder()
        .destinationAddress(event.incomingPreparePacket().getDestination())
        .transactionStatus(getTransactionStatus(event.streamPacket()))
        .packetCount(1)
        .referenceId(event.incomingPreparePacket().getDestination().getValue())
        .modifiedAt(Instant.now())
        .createdAt(Instant.now())
        .assetScale(event.denomination().assetScale())
        .assetCode(event.denomination().assetCode())
        .amount(event.incomingPreparePacket().getAmount())
        .accountId(event.accountId())
        .build());
  }

  private TransactionStatus getTransactionStatus(StreamPacket streamPacket) {
    if (streamPacket.frames().stream()
      .map(StreamFrame::streamFrameType)
      .anyMatch(CLOSING_FRAMES::contains)) {
      return TransactionStatus.CLOSED_BY_STREAM;
    } else {
      return TransactionStatus.PENDING;
    }
  }


}
