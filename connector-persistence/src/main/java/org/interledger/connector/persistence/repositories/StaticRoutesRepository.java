package org.interledger.connector.persistence.repositories;

import org.interledger.connector.persistence.entities.StaticRouteEntity;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StaticRoutesRepository extends CrudRepository<StaticRouteEntity, Long>, StaticRoutesRepositoryCustom {

  /**
   * Derived delete query per
   * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.modifying-queries.derived-delete
   * @param naturalId the unique natural id of the entity
   * @return list of entities deleted (should only be 1 or 0 due to unique constraint)
   */
  List<StaticRouteEntity> deleteByNaturalId(String naturalId);

  /**
   * Derived query for first by natural id per
   * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.limit-query-result
   * @param naturalId the unique natural id of the entity
   * @return the unique entity associated with the specified natural id
   */
  StaticRouteEntity findFirstByNaturalId(String naturalId);
}
