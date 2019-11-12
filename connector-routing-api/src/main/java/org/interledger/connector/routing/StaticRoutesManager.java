package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

import java.util.Set;

/**
 * A wrapper for the repository associated with static routes.
 */
public interface StaticRoutesManager {

  /**
   *
   * @return all static routes availabe in persistent storage
   */
  Set<StaticRoute> getAllStaticRoutes();

  /**
   * Removes a static route from persistence and the routing table
   * @param prefix the unique identifier of the route
   */
  void deleteStaticRouteByPrefix(InterledgerAddressPrefix prefix);

  /**
   * Creates a static route and makes sure the connector is aware of it as part of its routing
   * @param route
   * @return the created static route
   */
  StaticRoute createStaticRoute(StaticRoute route);

}
