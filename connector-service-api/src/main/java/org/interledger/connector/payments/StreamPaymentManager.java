package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing {@link StreamPayment}s. Business logic should interact with this instead of going to the
 * underlying persistence layer. This design allows stream payments to be persisted to different datastores
 * such as Postgres, Big Query, Spanner, etc without the business layer needing to know about the underlying datastore.
 */
public interface StreamPaymentManager {

  /**
   * Lists stream payments by accountId. Requests are paged to avoid accidentally fetching lots of stream payments.
   * @param accountId
   * @param pageRequest
   * @return
   */
  List<StreamPayment> findByAccountId(AccountId accountId, PageRequest pageRequest);

  /**
   * Find stream payment by accountId and reference id
   * @param accountId
   * @param streamPaymentId
   * @return
   */
  Optional<StreamPayment> findByAccountIdAndStreamPaymentId(AccountId accountId, String streamPaymentId);

  /**
   * Inserts or updates a stream payment. stream payments with the same {@link StreamPayment#accountId()} and
   * {@link StreamPayment#streamPaymentId()} are merged together.
   *
   * @param streamPayment
   */
  void merge(StreamPayment streamPayment);

}
