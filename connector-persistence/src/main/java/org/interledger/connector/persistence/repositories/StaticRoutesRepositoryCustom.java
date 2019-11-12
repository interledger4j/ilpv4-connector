package org.interledger.connector.persistence.repositories;

import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Set;

/**
 * Allows a {@link StaticRoutesRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface StaticRoutesRepositoryCustom {

  /**
   * Finds all static routes from the database and converts them to client-facing instances.
   * @return the converted database entities.
   */
  Set<StaticRoute> getAllStaticRoutes();

  /**
   * Saves a client-facing instance to the database and returns what was saved. Allows for creation but inherently
   * does not allow updates due to database constraints.
   * @param staticRoute a new static route
   * @return the new instance after saving
   */
  StaticRoute saveStaticRoute(StaticRoute staticRoute);

  /**
   * Deletes a static route by the prefix associated with it (a unique constraint)
   * @param prefix the unique identifier of the route
   * @return true if a route was deleted; false otherwise
   */
  boolean deleteStaticRoute(InterledgerAddressPrefix prefix);

  /**
   * Fetches a static route by the prefix associated with it (a unique constraint)
   * @param prefix the unique identifier of the route
   * @return the associated route if it exists
   */
  StaticRoute getByPrefix(InterledgerAddressPrefix prefix);
}
