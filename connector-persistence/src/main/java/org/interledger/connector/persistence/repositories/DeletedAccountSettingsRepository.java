package org.interledger.connector.persistence.repositories;

import org.interledger.connector.persistence.entities.DeletedAccountSettingsEntity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeletedAccountSettingsRepository extends CrudRepository<DeletedAccountSettingsEntity, Long> {
}
