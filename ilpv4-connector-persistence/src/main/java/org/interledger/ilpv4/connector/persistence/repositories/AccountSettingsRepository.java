package org.interledger.ilpv4.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
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
public interface AccountSettingsRepository extends CrudRepository<AccountSettingsEntity, Long> {

  Optional<AccountSettingsEntity> findByNaturalId(String naturalId);

  /**
   * Find AccountSettings for all accounts that initiate connections.
   *
   * @return A {@link List} of {@link AccountSettings}.
   */
  List<AccountSettingsEntity> findAccountSettingsEntitiesByConnectionInitiatorIsTrue();

  Optional<AccountSettingsEntity> findFirstByAccountRelationship(AccountRelationship relationship);

  /**
   * Find the first AccountSettings with a relationship of {@link AccountRelationship#PARENT}, if it exists.
   *
   * @return
   */
  default Optional<AccountSettingsEntity> findPrimaryParentAccountSettings() {
    return findFirstByAccountRelationship(AccountRelationship.PARENT);
  }

  /**
   * Find all AccountSettings of type {@link AccountRelationship#CHILD}.
   *
   * @param relationship The type of AccountSettings to find.
   *
   * @return An unordered collection of {@link AccountSettingsEntity} that are of type {@link
   * AccountRelationship#CHILD}.
   */
  Collection<AccountSettingsEntity> findByAccountRelationshipIs(AccountRelationship relationship);

  default Optional<AccountSettingsEntity> findByAccountId(AccountId accountId) {
    return this.findByNaturalId(accountId.value());
  }

  /**
   * Get the account settings for the specified {@code accountId} in a "safe" manner, meaning an exception will be
   * thrown if the account is not found.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return The requested {@link AccountSettingsEntity}, if present.
   */
  default AccountSettingsEntity safeFindByAccountId(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    // TODO: Use AccountSettingsNotFoundException
    return this.findByAccountId(accountId)
      .orElseThrow(() -> new RuntimeException("No AccountSettings found for AccountId: " + accountId));
  }

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

    return this.findByNaturalId(accountId.value())
      .map(AccountSettings::isInternal)
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
