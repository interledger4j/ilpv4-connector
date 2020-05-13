package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.StreamPaymentsRepository;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link StreamPaymentManager} that persists to a SQL database.
 */
public class InDatabaseStreamPaymentManager implements StreamPaymentManager {

  private final StreamPaymentsRepository streamPaymentsRepository;

  private final StreamPaymentFromEntityConverter streamPaymentFromEntityConverter;

  private final StreamPaymentToEntityConverter streamPaymentToEntityConverter;

  public InDatabaseStreamPaymentManager(StreamPaymentsRepository streamPaymentsRepository,
                                        StreamPaymentFromEntityConverter streamPaymentFromEntityConverter,
                                        StreamPaymentToEntityConverter streamPaymentToEntityConverter) {
    this.streamPaymentsRepository = streamPaymentsRepository;
    this.streamPaymentFromEntityConverter = streamPaymentFromEntityConverter;
    this.streamPaymentToEntityConverter = streamPaymentToEntityConverter;
  }

  @Override
  public List<StreamPayment> findByAccountId(AccountId accountId, PageRequest pageRequest) {
    return streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, pageRequest)
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
  }

}
