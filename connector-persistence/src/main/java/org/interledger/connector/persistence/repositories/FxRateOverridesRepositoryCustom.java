package org.interledger.connector.persistence.repositories;

import org.interledger.connector.fxrates.FxRateOverride;

import java.util.Set;

/**
 * Allows a {@link FxRateOverridesRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface FxRateOverridesRepositoryCustom {

  /**
   *
   * @return all overrides available from the database, converted to instances appropriate to return to the client.
   */
  Set<FxRateOverride> getAllOverrides();

  /**
   * Save all of the overrides, meaning:
   * - Updates to existing
   * - Deletion of those missing from the set
   * - Insertion of those present in the set and missing in the database
   *
   * @param overrides the user specified overrides
   * @return the newly saved entities after any database initialized values have been assigned to fields.
   */
  Set<FxRateOverride> saveAllOverrides(Set<FxRateOverride> overrides);
}
