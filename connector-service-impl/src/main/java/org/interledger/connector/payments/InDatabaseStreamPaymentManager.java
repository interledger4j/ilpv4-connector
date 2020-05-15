package org.interledger.connector.payments;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.StreamPaymentEntity;
import org.interledger.connector.persistence.repositories.StreamPaymentsRepository;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link StreamPaymentManager} that persists to a SQL database.
 */
public class InDatabaseStreamPaymentManager implements StreamPaymentManager {

  private static final Logger LOGGER = getLogger(InDatabaseStreamPaymentManager.class);

  private final StreamPaymentsRepository streamPaymentsRepository;

  private final StreamPaymentFromEntityConverter streamPaymentFromEntityConverter;

  private final StreamPaymentToEntityConverter streamPaymentToEntityConverter;

  // FIXME replace with bridging service
  private final EventBus eventBus;


  public InDatabaseStreamPaymentManager(StreamPaymentsRepository streamPaymentsRepository,
                                        StreamPaymentFromEntityConverter streamPaymentFromEntityConverter,
                                        StreamPaymentToEntityConverter streamPaymentToEntityConverter,
                                        EventBus eventBus) {
    this.streamPaymentsRepository = streamPaymentsRepository;
    this.streamPaymentFromEntityConverter = streamPaymentFromEntityConverter;
    this.streamPaymentToEntityConverter = streamPaymentToEntityConverter;
    this.eventBus = eventBus;
  }

  @Override
  public List<StreamPayment> findByAccountId(AccountId accountId, PageRequest pageRequest) {
    return streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, pageRequest)
      .stream()
      .map(streamPaymentFromEntityConverter::convert)
      .collect(Collectors.toList());
  }

  @Override
  public List<StreamPayment> findByAccountIdAndCorrelationId(AccountId accountId,
                                                             String correlationId,
                                                             PageRequest pageRequest) {
    return streamPaymentsRepository.findByAccountIdAndCorrelationIdOrderByCreatedDateDesc(
      accountId,
      correlationId,
      pageRequest)
      .stream()
      .map(streamPaymentFromEntityConverter::convert)
      .collect(Collectors.toList());
  }

  @Override
  public Optional<StreamPayment> findByAccountIdAndStreamPaymentId(AccountId accountId, String streamPaymentId) {
    return streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId)
      .map(streamPaymentFromEntityConverter::convert);
  }

  @Override
  public void merge(StreamPayment streamPayment) {
    streamPaymentsRepository.upsertAmounts(streamPaymentToEntityConverter.convert(streamPayment));
    streamPayment.sourceAddress().ifPresent(sourceAddress -> {
      streamPaymentsRepository.updateSourceAddress(streamPayment.accountId(),
        streamPayment.streamPaymentId(),
        sourceAddress.getValue());
    });
    streamPayment.deliveredAssetScale().ifPresent(assetScale -> {
      streamPaymentsRepository.udpdateDeliveredDenomination(streamPayment.accountId(),
        streamPayment.streamPaymentId(),
          streamPayment.deliveredAssetCode().orElse("unknown"),
          assetScale);
    });
    if (!streamPayment.status().equals(StreamPaymentStatus.PENDING)) {
      streamPaymentsRepository.updateStatus(streamPayment.accountId(),
        streamPayment.streamPaymentId(),
        streamPayment.status());
    }
    if (streamPayment.status().equals(StreamPaymentStatus.CLOSED_BY_STREAM)) {
      streamPaymentsRepository
        .findByAccountIdAndStreamPaymentId(streamPayment.accountId(), streamPayment.streamPaymentId())
        .map(this::notifyReceivedPaymentClosed);
    }
  }

  private StreamPayment notifyReceivedPaymentClosed(StreamPaymentEntity streamPaymentEntity) {
    StreamPayment streamPayment = streamPaymentFromEntityConverter.convert(streamPaymentEntity);
    try {
      eventBus.post(ClosedPaymentEvent.builder().payment(streamPayment).build());
    } catch (Exception e) {
      LOGGER.error("Error notifying invoiceService about payment {}", streamPayment, e);
    }
    return streamPayment;
  }

}
