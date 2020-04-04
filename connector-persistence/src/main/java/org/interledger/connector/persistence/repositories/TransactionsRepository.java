package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.TransactionEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Allows AccessTokens to be persisted to a datastore.
 */
@Repository
public interface TransactionsRepository
  extends PagingAndSortingRepository<TransactionEntity, Long> {

  /**
   * Find an list {@link TransactionEntity} by accountId order by creation date desc
   *
   * @param accountId An id corresponding to {@link TransactionEntity#getAccountId()}.
   * @param pageable page request
   *
   * @return List of {@link TransactionEntity}.
   */
  List<TransactionEntity> findByAccountIdOrderByCreatedDateDesc(AccountId accountId, Pageable pageable);


  /**
   * Find an {@link TransactionEntity} by its natural identifier corresponding to
   * {@link TransactionEntity#getReferenceId()}.
   *
   * @param referenceId
   * @return record if found
   */
  Optional<TransactionEntity> findByReferenceId(String referenceId);

}
