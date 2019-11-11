package org.interledger.connector.persistence.converters;

import org.interledger.connector.persistence.entities.StaticRouteEntity;
import org.interledger.connector.routing.StaticRoute;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class StaticRouteEntityConverter implements Converter<StaticRouteEntity, StaticRoute> {

  @Override
  public StaticRoute convert(StaticRouteEntity staticRouteEntity) {
    Objects.requireNonNull(staticRouteEntity);

    return StaticRoute.builder()
        .accountId(staticRouteEntity.getBoxedAccountId())
        .addressPrefix(staticRouteEntity.getPrefix())
        .id(staticRouteEntity.getId())
        .createdAt(Optional.ofNullable(staticRouteEntity.getCreatedDate()).orElseGet(() -> Instant.now()))
        .modifiedAt(Optional.ofNullable(staticRouteEntity.getModifiedDate()).orElseGet(() -> Instant.now()))
        .build();
  }
}
