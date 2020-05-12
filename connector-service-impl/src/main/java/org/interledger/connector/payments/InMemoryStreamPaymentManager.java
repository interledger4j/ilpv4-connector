package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;

import org.immutables.value.Value;
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

  // merge is synchronized so no need for concurrent hashmap
  private final Map<MapKey, StreamPayment> transactionsMap = new HashMap<>();

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
    }
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
