package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Allows a {@link AccountSettingsRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface AccountSettingsRepositoryCustom {

  /**
   * Performs a database lookup using the supplied {@code accountId}. If the entity is found, it is then converted to an
   * instance of {@link AccountSettings}. Otherwise, {@link Optional#empty()} is returned.
   *
   * @param accountId The {@link AccountId} to query for.
   *
   * @return
   */
  Optional<AccountSettings> findByAccountIdWithConversion(AccountId accountId);

  /**
   * Find the first {@link AccountSettings} that this connector has with a relationship of {@code relationship}.
   *
   * @param relationship An {@link AccountRelationship} to filter by.
   *
   * @return An optionally-present {@link AccountSettings}.
   */
  Optional<AccountSettings> findFirstByAccountRelationshipWithConversion(AccountRelationship relationship);

  /**
   * Find all {@link AccountSettings} of type {@code relationship}.
   *
   * @param relationship The type of {@link AccountSettings} to find.
   *
   * @return An unordered collection of {@link AccountSettingsEntity} that are of type {@code relationship}.
   */
  Collection<AccountSettings> findByAccountRelationshipIsWithConversion(AccountRelationship relationship);

  /**
   * Performs a database lookup to find all account settings objects for any accounts that initiate connections.
   *
   * @return A {@link List} of {@link AccountSettings}.
   */
  List<AccountSettings> findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion();

  /**
   * Find an {@link AccountSettings} by its settlement engine identifier.
   *
   * @param settlementEngineAccountId A {@link SettlementEngineAccountId} to index by.
   *
   * @return An optionally present {@link AccountSettings}.
   */
  Optional<AccountSettings> findBySettlementEngineAccountIdWithConversion(
    SettlementEngineAccountId settlementEngineAccountId
  );
}
