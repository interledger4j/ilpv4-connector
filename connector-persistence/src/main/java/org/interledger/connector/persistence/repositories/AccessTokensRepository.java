package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.AccessTokenEntity;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Allows AccessTokens to be persisted to a datastore.
 */
@Repository
public interface AccessTokensRepository
  extends CrudRepository<AccessTokenEntity, Long>, AccessTokensRepositoryCustom {

  /**
   * Find an {@link AccessTokenEntity} by its natural identifier (i.e., the accountId as a String).
   *
   * @param accountId A {@link String} corresponding to {@link AccessTokenEntity#getAccountId()}.
   * @param id id corresponding to {@link AccessTokenEntity#getId()} ()}.
   *
   * @return optional {@link AccessTokenEntity}.
   */
  Optional<AccessTokenEntity> findByAccountIdAndId(AccountId accountId, long id);

  /**
   * Find an {@link AccessTokenEntity} by its natural identifier (i.e., the accountId as a String).
   *
   * @param accountId A {@link String} corresponding to {@link AccessTokenEntity#getAccountId()}.
   *
   * @return List of {@link AccountSettingsEntity}.
   */
  List<AccessTokenEntity> findByAccountId(AccountId accountId);

  void deleteByAccountId(AccountId accountId);

  void deleteByAccountIdAndId(AccountId accountId, Long id);
}
