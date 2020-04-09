package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.StreamPaymentEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Read-only repository to fetch TransactionEntity from the database. For write operations,
 * use {@link StreamPaymentsRepositoryCustom} instead.
 */
@org.springframework.stereotype.Repository
public interface StreamPaymentsRepository extends Repository<StreamPaymentEntity, Long>, StreamPaymentsRepositoryCustom {

  /**
   * Find an list {@link StreamPaymentEntity} by accountId order by creation date desc
   *
   * @param accountId An id corresponding to {@link StreamPaymentEntity#getAccountId()}.
   * @param pageable page request
   *
   * @return List of {@link StreamPaymentEntity}.
   */
  List<StreamPaymentEntity> findByAccountIdOrderByCreatedDateDesc(AccountId accountId, Pageable pageable);


  /**
   * Find an {@link StreamPaymentEntity} by its natural identifier corresponding to
   * {@link AccountId} and {@link StreamPaymentEntity#getStreamPaymentId()}.
   *
   * @param accountId
   * @param streamPaymentId
   * @return record if found
   */
  Optional<StreamPaymentEntity> findByAccountIdAndStreamPaymentId(AccountId accountId, String streamPaymentId);

}
