package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing {@link Transaction}s. Business logic should interact with this instead of going to the
 * underlying persistence layer. This design allows transactions to be persisted to different datastores
 * such as Postgres, Big Query, Spanner, etc without the business layer needing to about the underlying datastore.
 */
public interface PaymentTransactionManager {

  /**
   * Lists transactions by accountId. Requests are paged to avoid accidentally fetching lots of transactions.
   * @param accountId
   * @param pageRequest
   * @return
   */
  List<Transaction> findByAccountId(AccountId accountId, PageRequest pageRequest);

  /**
   * Find transaction by accountId and reference id
   * @param accountId
   * @param referenceId
   * @return
   */
  Optional<Transaction> findByAccountIdAndReferenceId(AccountId accountId, String referenceId);

  /**
   * Inserts or updates a transaction. Transactions with the same {@link Transaction#accountId()} and
   * {@link Transaction#referenceId()} are merged together.
   *
   * @param transaction
   */
  void merge(Transaction transaction);

}
