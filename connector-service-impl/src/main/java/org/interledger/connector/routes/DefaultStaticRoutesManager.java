package org.interledger.connector.routes;

import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Objects;
import java.util.Set;

public class DefaultStaticRoutesManager implements StaticRoutesManager {

  private final StaticRoutesRepository staticRoutesRepository;

  public DefaultStaticRoutesManager(StaticRoutesRepository staticRoutesRepository) {
    this.staticRoutesRepository = staticRoutesRepository;
  }

  @Override
  public Set<StaticRoute> getAllRoutesUncached() {
    return staticRoutesRepository.getAllStaticRoutes();
  }


  @Override
  public void deleteByPrefix(InterledgerAddressPrefix prefix) {
    Objects.requireNonNull(prefix);
    staticRoutesRepository.deleteStaticRoute(prefix);
  }

  @Override
  public Set<StaticRoute> updateAll(Set<StaticRoute> routes) {
    Objects.requireNonNull(routes);
    Set<StaticRoute> savedRoutes = staticRoutesRepository.saveAllStaticRoutes(routes);
    return savedRoutes;
  }

  @Override
  public StaticRoute update(StaticRoute route) {
    Objects.requireNonNull(route);
    StaticRoute saved = staticRoutesRepository.saveStaticRoute(route);
    return saved;
  }
}
