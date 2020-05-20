package org.interledger.connector.payments;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.StreamPaymentClosedEvent;

import com.google.common.eventbus.EventBus;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of {@link StreamPaymentManager} that store payments in memory and does not persist
 * across restarts. Intended for local dev where a persistent datastore is not being used.
 */
public class InMemoryStreamPaymentManager implements StreamPaymentManager {

  private static final Logger LOGGER = getLogger(InMemoryStreamPaymentManager.class);

  // merge is synchronized so no need for concurrent hashmap
  private final Map<MapKey, StreamPayment> transactionsMap = new HashMap<>();

  private final EventBus eventBus;

  public InMemoryStreamPaymentManager(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public List<StreamPayment> findByAccountId(AccountId accountId, PageRequest pageRequest) {
    return transactionsMap.values()
      .stream()
      .filter(trx -> trx.accountId().equals(accountId))
      .skip(pageRequest.getOffset())
      .limit(pageRequest.getPageSize())
      .collect(Collectors.toList());
  }

  @Override
  public Optional<StreamPayment> findByAccountIdAndStreamPaymentId(AccountId accountId, String streamPaymentId) {
    MapKey key = MapKey.of(accountId, streamPaymentId);
    return Optional.ofNullable(transactionsMap.get(key));
  }

  @Override
  public List<StreamPayment> findByAccountIdAndCorrelationId(AccountId accountId,
                                                             String correlationId,
                                                             PageRequest pageRequest) {
    return transactionsMap.values()
      .stream()
      .filter(trx -> trx.accountId().equals(accountId))
      .filter(trx -> trx.correlationId().equals(correlationId))
      .skip(pageRequest.getOffset())
      .limit(pageRequest.getPageSize())
      .collect(Collectors.toList());
  }

  @Override
  public synchronized void merge(StreamPayment streamPayment) {
    StreamPayment merged = upsertAmounts(streamPayment);
    if (streamPayment.deliveredAssetCode().isPresent()) {
      merged = put(StreamPayment.builder().from(merged)
        .deliveredAssetCode(streamPayment.deliveredAssetCode())
        .deliveredAssetScale(streamPayment.deliveredAssetScale())
        .build());
    }
    if (!streamPayment.status().equals(StreamPaymentStatus.PENDING)) {
      put(StreamPayment.builder().from(merged)
        .status(streamPayment.status())
        .build());
      merged = transactionsMap.get(MapKey.of(streamPayment.accountId(), streamPayment.streamPaymentId()));
    }
    if (merged.status().equals(StreamPaymentStatus.CLOSED_BY_STREAM)) {
      try {
        eventBus.post(StreamPaymentClosedEvent.builder().streamPayment(merged).build());
      } catch (Exception e) {
        LOGGER.error("Error notifying invoiceService about payment {}", streamPayment, e);
      }
    }
  }

  @Override
  public List<StreamPayment> closeIdlePendingPayments(Instant idleSince) {
    return transactionsMap.values()
      .stream()
      .filter(trx -> trx.status().equals(StreamPaymentStatus.PENDING))
      .filter(trx -> trx.modifiedAt().isBefore(idleSince))
      .map(payment -> updateExistingStatus(payment, StreamPaymentStatus.CLOSED_BY_EXPIRATION))
      .collect(Collectors.toList());
  }

  @Override
  public boolean updateStatus(AccountId accountId, String streamPaymentId, StreamPaymentStatus status) {
    return findByAccountIdAndStreamPaymentId(accountId, streamPaymentId)
      .map(existing -> updateExistingStatus(existing, status))
      .map($ -> true)
      .orElse(false);
  }

  private StreamPayment updateExistingStatus(StreamPayment existing, StreamPaymentStatus newStatus) {
    return put(StreamPayment.builder().from(existing)
      .status(newStatus)
      .modifiedAt(Instant.now())
      .build());
  }

  private StreamPayment upsertAmounts(StreamPayment streamPayment) {
    return findByAccountIdAndStreamPaymentId(streamPayment.accountId(), streamPayment.streamPaymentId())
      .map(existing -> put(StreamPayment.builder().from(existing)
        .amount(existing.amount().add(streamPayment.amount()))
        .deliveredAmount(existing.deliveredAmount().plus(streamPayment.deliveredAmount()))
        .packetCount(existing.packetCount() + streamPayment.packetCount())
        .modifiedAt(Instant.now())
        .build()))
      .orElseGet(() -> put(streamPayment));
  }

  private StreamPayment put(StreamPayment streamPayment) {
    MapKey key = MapKey.of(streamPayment.accountId(), streamPayment.streamPaymentId());
    transactionsMap.put(key, streamPayment);
    return streamPayment;
  }

  @Value.Immutable
  public interface MapKey {
    static MapKey of(AccountId accountId, String streamPaymentId) {
      return ImmutableMapKey.builder()
        .accountId(accountId)
        .streamPaymentId(streamPaymentId)
        .build();
    }

    AccountId accountId();

    String streamPaymentId();
  }

}
