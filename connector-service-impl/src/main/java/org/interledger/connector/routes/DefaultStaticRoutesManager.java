package org.interledger.connector.routes;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.caching.StaticRoutesLoadingCache;
import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Objects;
import java.util.Set;

public class DefaultStaticRoutesManager implements StaticRoutesManager {

  private final StaticRoutesRepository staticRoutesRepository;

  private final StaticRoutesLoadingCache staticRoutesCache;

  public DefaultStaticRoutesManager(StaticRoutesRepository staticRoutesRepository,
                                    StaticRoutesLoadingCache staticRoutesCache) {
    this.staticRoutesRepository = staticRoutesRepository;
    this.staticRoutesCache = staticRoutesCache;
  }

  @Override
  public Set<StaticRoute> getAllRoutesUncached() {
    return staticRoutesRepository.getAllStaticRoutes();
  }

  @Override
  public AccountId getNextHopAccountId(InterledgerAddressPrefix prefix) {
    Objects.requireNonNull(prefix);
    return staticRoutesCache.getNextHopAccountIdForPrefix(prefix);
  }

  @Override
  public void deleteByPrefix(InterledgerAddressPrefix prefix) {
    Objects.requireNonNull(prefix);
    staticRoutesRepository.deleteStaticRoute(prefix);
    staticRoutesCache.removeStaticRoute(prefix);
  }

  @Override
  public Set<StaticRoute> updateAll(Set<StaticRoute> routes) {
    Objects.requireNonNull(routes);
    Set<StaticRoute> savedRoutes = staticRoutesRepository.saveAllStaticRoutes(routes);
    staticRoutesCache.refreshAll();
    return savedRoutes;
  }

  @Override
  public StaticRoute update(StaticRoute route) {
    Objects.requireNonNull(route);
    StaticRoute saved = staticRoutesRepository.saveStaticRoute(route);
    staticRoutesCache.refreshStaticRoute(saved);
    return saved;
  }
}
