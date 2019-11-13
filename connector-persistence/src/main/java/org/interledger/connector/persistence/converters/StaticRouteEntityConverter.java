package org.interledger.connector.persistence.converters;

import org.interledger.connector.persistence.entities.StaticRouteEntity;
import org.interledger.connector.routing.StaticRoute;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A converter from {@link StaticRouteEntity} to {@link StaticRoute}
 */
public class StaticRouteEntityConverter implements Converter<StaticRouteEntity, StaticRoute> {

  @Override
  public StaticRoute convert(StaticRouteEntity staticRouteEntity) {
    Objects.requireNonNull(staticRouteEntity);

    return StaticRoute.builder()
        .nextHopAccountId(staticRouteEntity.getBoxedAccountId())
        .routePrefix(staticRouteEntity.getPrefix())
        // Entity created and modified dates are automatically set by Spring Data, and are technically read-only from
        // the perspective of a normal Java developer. However, for testing purposes, we need a default value because
        // these dates will be null if an entity is not created by Spring Data.
        .createdAt(Optional.ofNullable(staticRouteEntity.getCreatedDate()).orElseGet(() -> Instant.now()))
        .modifiedAt(Optional.ofNullable(staticRouteEntity.getModifiedDate()).orElseGet(() -> Instant.now()))
        .build();
  }
}
