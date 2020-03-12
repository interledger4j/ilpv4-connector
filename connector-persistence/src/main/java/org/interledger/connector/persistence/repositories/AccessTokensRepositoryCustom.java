package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccessToken;
import org.interledger.connector.persistence.entities.AccessTokenEntity;

import java.util.List;
import java.util.Optional;

/**
 * Allows a {@link AccessTokensRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface AccessTokensRepositoryCustom {

  List<AccessToken> withConversion(List<AccessTokenEntity> tokens);

  Optional<AccessToken> withConversion(Optional<AccessTokenEntity> token);

  AccessToken withConversion(AccessTokenEntity token);

}
