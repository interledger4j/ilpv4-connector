package org.interledger.connector.routes;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Set;

public interface StaticRoutesManager {

  Set<StaticRoute> getAllRoutesUncached();

  AccountId getNextHopAccountId(InterledgerAddressPrefix prefix);

  void deleteByPrefix(InterledgerAddressPrefix prefix);

  Set<StaticRoute> updateAll(Set<StaticRoute> routes);

  StaticRoute update(StaticRoute route);

}
