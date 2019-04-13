package org.interledger.ilpv4.connector.persistence.repository;

import org.interledger.ilpv4.connector.persistence.model.AccountEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Allows Accounts to be persisted to a datastore.
 */
@Repository
public interface AccountRepository extends CrudRepository<AccountEntity, String> {
}
