package org.interledger.connector.routes;

import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.routing.StaticRouteAlreadyExistsProblem;
import org.interledger.connector.routing.StaticRouteNotFoundProblem;
import org.interledger.core.InterledgerAddressPrefix;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Objects;
import java.util.Set;

public class DefaultStaticRoutesManager implements StaticRoutesManager {

  private final StaticRoutesRepository staticRoutesRepository;

  public DefaultStaticRoutesManager(StaticRoutesRepository staticRoutesRepository) {
    this.staticRoutesRepository = staticRoutesRepository;
  }

  @Override
  public Set<StaticRoute> getAll() {
    return staticRoutesRepository.getAllStaticRoutes();
  }


  @Override
  public void deleteByPrefix(InterledgerAddressPrefix prefix) {
    Objects.requireNonNull(prefix);
    if (!staticRoutesRepository.deleteStaticRoute(prefix)) {
      throw new StaticRouteNotFoundProblem(prefix);
    }
  }

  @Override
  public StaticRoute update(StaticRoute route) {
    Objects.requireNonNull(route);
    try {
      StaticRoute saved = staticRoutesRepository.saveStaticRoute(route);
      return saved;
    }
    catch(Exception e) {
      if (e.getCause() instanceof ConstraintViolationException) {
        throw new StaticRouteAlreadyExistsProblem(route.addressPrefix());
      }
      throw e;
    }

  }
}
