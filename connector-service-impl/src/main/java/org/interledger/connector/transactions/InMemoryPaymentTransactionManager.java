package org.interledger.connector.transactions;

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
 * Implementation of {@link PaymentTransactionManager} that store transactions in memory and does not persist
 * across restarts. Intended for local dev where a persistent datastore is not being used.
 */
public class InMemoryPaymentTransactionManager implements PaymentTransactionManager {

  // merge is synchronized so no need for concurrent hashmap
  private final Map<MapKey, Transaction> transactionsMap = new HashMap<>();

  @Override
  public List<Transaction> findByAccountId(AccountId accountId, PageRequest pageRequest) {
    return transactionsMap.values()
      .stream()
      .filter(trx -> trx.accountId().equals(accountId))
      .skip(pageRequest.getOffset())
      .limit(pageRequest.getPageSize())
      .collect(Collectors.toList());
  }

  @Override
  public Optional<Transaction> findByAccountIdAndTransactionId(AccountId accountId, String transactionId) {
    MapKey key = MapKey.of(accountId, transactionId);
    return Optional.ofNullable(transactionsMap.get(key));
  }

  @Override
  public synchronized void merge(Transaction transaction) {
    Transaction merged = upsertAmounts(transaction);
    if (transaction.sourceAddress().isPresent()) {
      merged = put(Transaction.builder().from(merged)
        .sourceAddress(transaction.sourceAddress())
        .build());
    }
    if (!transaction.status().equals(TransactionStatus.PENDING)) {
      put(Transaction.builder().from(merged)
        .status(transaction.status())
        .build());
    }
  }

  private Transaction upsertAmounts(Transaction transaction) {
    return findByAccountIdAndTransactionId(transaction.accountId(), transaction.transactionId())
      .map(existing -> put(Transaction.builder().from(existing)
        .amount(existing.amount().add(transaction.amount()))
        .packetCount(existing.packetCount() + transaction.packetCount())
        .modifiedAt(Instant.now())
        .build()))
      .orElseGet(() -> put(transaction));
  }

  private Transaction put(Transaction transaction) {
    MapKey key = MapKey.of(transaction.accountId(), transaction.transactionId());
    transactionsMap.put(key, transaction);
    return transaction;
  }

  @Value.Immutable
  public interface MapKey {
    static MapKey of(AccountId accountId, String transactionId) {
      return ImmutableMapKey.builder()
        .accountId(accountId)
        .transactionId(transactionId)
        .build();
    }

    AccountId accountId();
    String transactionId();
  }

}
