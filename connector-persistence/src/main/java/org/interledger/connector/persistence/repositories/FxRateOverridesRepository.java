package org.interledger.connector.persistence.repositories;

import org.interledger.connector.persistence.entities.FxRateOverrideEntity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FxRateOverridesRepository extends CrudRepository<FxRateOverrideEntity, Long>,
    FxRateOverridesRepositoryCustom {

}
