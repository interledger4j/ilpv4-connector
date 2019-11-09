package org.interledger.connector.persistence.repositories;

import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Set;

public interface StaticRoutesRepositoryCustom {

  Set<StaticRoute> getAllStaticRoutes();

  StaticRoute saveStaticRoute(StaticRoute staticRoute);

  void deleteStaticRoute(InterledgerAddressPrefix prefix);

  StaticRoute getByPrefix(InterledgerAddressPrefix prefix);
}
