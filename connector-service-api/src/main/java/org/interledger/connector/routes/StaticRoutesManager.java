package org.interledger.connector.routes;

import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Set;

/**
 * A wrapper for the repository associated with static routes. Currently functioning as a mere proxy but may end up
 * incorporating mechanisms for updating cached results later on.
 */
public interface StaticRoutesManager {

  Set<StaticRoute> getAllRoutesUncached();

  void deleteByPrefix(InterledgerAddressPrefix prefix);

  Set<StaticRoute> updateAll(Set<StaticRoute> routes);

  StaticRoute update(StaticRoute route);

}
