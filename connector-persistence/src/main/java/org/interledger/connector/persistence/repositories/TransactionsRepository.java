package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.TransactionEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Read-only repository to fetch TransactionEntity from the database. For write operations,
 * use {@link TransactionsRepositoryCustom} instead.
 */
@org.springframework.stereotype.Repository
public interface TransactionsRepository extends Repository<TransactionEntity, Long>, TransactionsRepositoryCustom {

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
   * {@link AccountId} and {@link TransactionEntity#getReferenceId()}.
   *
   * @param accountId
   * @param referenceId
   * @return record if found
   */
  Optional<TransactionEntity> findByAccountIdAndReferenceId(AccountId accountId, String referenceId);

}
