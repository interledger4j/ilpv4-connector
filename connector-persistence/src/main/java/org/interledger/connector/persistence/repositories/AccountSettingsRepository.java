package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Allows Accounts to be persisted to a datastore.
 */
@Repository
public interface AccountSettingsRepository
  extends CrudRepository<AccountSettingsEntity, Long>, AccountSettingsRepositoryCustom {

  /**
   * Find an {@link AccountSettingsEntity} by its natural identifier (i.e., the accountId as a String).
   *
   * @param accountId A {@link String} corresponding to {@link AccountSettingsEntity#getAccountId()}.
   *
   * @return An optionally-present {@link AccountSettingsEntity}.
   */
  Optional<AccountSettingsEntity> findByAccountId(String accountId);

  /**
   * Find an {@link AccountSettingsEntity} by the supplied {@code accountId}.
   *
   * @param accountId The {@link AccountId} to lookup by.
   *
   * @return An optionally-present {@link AccountSettingsEntity}.
   */
  default Optional<AccountSettingsEntity> findByAccountId(AccountId accountId) {
    return this.findByAccountId(accountId.value());
  }

  /**
   * Get the {@link AccountSettingsEntity} for the specified {@code accountId} in a "safe" manner, meaning an exception
   * will be thrown if the account is not found.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return The requested {@link AccountSettingsEntity}, if present.
   */
  default AccountSettingsEntity safeFindByAccountId(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return this.findByAccountId(accountId)
      .orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

  /**
   * Find an {@link AccountSettingsEntity} by its settlement engine identifier.
   *
   * @param settlementEngineAccountId A {@link SettlementEngineAccountId} to index by.
   *
   * @return An optionally present {@link AccountSettingsEntity}.
   */
  default Optional<AccountSettingsEntity> findBySettlementEngineAccountId(
    SettlementEngineAccountId settlementEngineAccountId
  ) {
    return this.findAccountSettingsEntityBySettlementEngineDetailsSettlementEngineAccountId(
      settlementEngineAccountId.value()
    );
  }

  /**
   * Find an {@link AccountSettingsEntity} by its settlement engine identifier.
   *
   * @param settlementEngineAccountId A {@link String} representation to index by.
   *
   * @return An optionally present {@link AccountSettingsEntity}.
   */
  Optional<AccountSettingsEntity> findAccountSettingsEntityBySettlementEngineDetailsSettlementEngineAccountId(
    String settlementEngineAccountId
  );

  /**
   * Find all {@link AccountSettingsEntity} objects for all accounts that initiate connections.
   *
   * @return A {@link List} of {@link AccountSettings}.
   */
  List<AccountSettingsEntity> findAccountSettingsEntitiesByConnectionInitiatorIsTrue();

  /**
   * Find the first {@link AccountSettingsEntity} that this connector has with a relationship of {@code relationship}.
   *
   * @param relationship An {@link AccountRelationship} to filter by.
   *
   * @return An optionally-present {@link AccountSettingsEntity}.
   */
  Optional<AccountSettingsEntity> findFirstByAccountRelationship(AccountRelationship relationship);

  /**
   * Find the first {@link AccountSettingsEntity} with a relationship of {@link AccountRelationship#PARENT}, if it
   * exists.
   *
   * @return An optionally-present {@link AccountSettingsEntity}.
   */
  default Optional<AccountSettingsEntity> findPrimaryParentAccountSettings() {
    return findFirstByAccountRelationship(AccountRelationship.PARENT);
  }

  /**
   * Find all {@link AccountSettingsEntity} of type {@link AccountRelationship#CHILD}.
   *
   * @param relationship The type of AccountSettings to find.
   *
   * @return An unordered collection of {@link AccountSettingsEntity} that are of type {@link
   * AccountRelationship#CHILD}.
   */
  Collection<AccountSettingsEntity> findByAccountRelationshipIs(AccountRelationship relationship);

  /**
   * Determines if the account represented by {@code accountId} is an internal account. This method is provided for
   * other services that only have an {@link AccountId} that may not wish to load the AccountSettings in all code-paths
   * (e .g., see `InterledgerAddressUtils`).
   *
   * @param accountId The {@link AccountId} of the Account to check.
   *
   * @return {@code true} or {@code false}, or {@link Optional#empty()} if the account doesn't exist.
   */
  default Optional<Boolean> isInternal(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    return this.findByAccountId(accountId.value())
      .map(AccountSettingsEntity::isInternal)
      .map(Optional::ofNullable)
      .orElse(Optional.empty());
  }

  /**
   * Determines if the account represented by {@code accountId} is internal. This method is provided for other services
   * that only have an {@link AccountId} that may not wish to load the AccountSettings in all code-paths (e .g., see
   * `InterledgerAddressUtils`).
   *
   * @param accountId The {@link AccountId} of the Account to check.
   *
   * @return {@code true} or {@code false}, or {@link Optional#empty()} if the account doesn't exist.
   */
  default Optional<Boolean> isNotInternal(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    return this.isInternal(accountId)
      .map(isInternal -> !isInternal);
  }

}
