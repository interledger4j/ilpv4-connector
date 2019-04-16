package org.interledger.ilpv4.connector.persistence.repositories;

import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Allows Accounts to be persisted to a datastore.
 */
@Repository
public interface AccountSettingsRepository extends CrudRepository<AccountSettingsEntity, Long> {

  Optional<AccountSettingsEntity> findByNaturalId(UUID naturalId);

}
