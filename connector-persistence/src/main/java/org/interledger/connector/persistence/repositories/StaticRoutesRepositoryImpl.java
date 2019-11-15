package org.interledger.connector.persistence.repositories;

import org.interledger.connector.persistence.entities.StaticRouteEntity;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An extension of {@link StaticRoutesRepository} for use by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public class StaticRoutesRepositoryImpl implements StaticRoutesRepositoryCustom {

  @Autowired
  @Lazy
  private ConversionService conversionService;

  @Autowired
  private StaticRoutesRepository staticRoutesRepository;

  @Override
  public Set<StaticRoute> getAllStaticRoutes() {
    Iterable<StaticRouteEntity> staticRoutes = staticRoutesRepository.findAll();
    return mapEntitiesToDtos(staticRoutes);
  }

  @Override
  public StaticRoute saveStaticRoute(StaticRoute staticRoute) {
    Objects.requireNonNull(staticRoute);
    StaticRouteEntity saved = staticRoutesRepository.save(new StaticRouteEntity(staticRoute));
    return conversionService.convert(saved, StaticRoute.class);
  }

  @Override
  @Transactional
  public boolean deleteStaticRouteByPrefix(InterledgerAddressPrefix prefix) {
    Objects.requireNonNull(prefix);
    return !staticRoutesRepository.deleteByAddressPrefix(prefix.getValue()).isEmpty();
  }

  @Override
  public StaticRoute findByAddressPrefix(InterledgerAddressPrefix prefix) {
    Objects.requireNonNull(prefix);
    StaticRouteEntity staticRoute = staticRoutesRepository.findFirstByAddressPrefix(prefix.getValue());
    return conversionService.convert(staticRoute, StaticRoute.class);
  }

  private Set<StaticRoute> mapEntitiesToDtos(Iterable<StaticRouteEntity> overrides) {
    return StreamSupport.stream(overrides.spliterator(), false)
        .map(s -> conversionService.convert(s, StaticRoute.class))
        .collect(Collectors.toSet());
  }
}
