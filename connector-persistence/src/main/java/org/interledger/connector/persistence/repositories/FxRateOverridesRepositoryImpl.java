package org.interledger.connector.persistence.repositories;

import org.interledger.connector.fxrates.FxRateOverride;
import org.interledger.connector.persistence.entities.FxRateOverrideEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FxRateOverridesRepositoryImpl implements FxRateOverridesRepositoryCustom {

  @Autowired
  @Lazy
  private ConversionService conversionService;

  @Autowired
  private FxRateOverridesRepository fxRateOverridesRepository;

  @Override
  public Set<FxRateOverride> getAllOverrides() {
    Iterable<FxRateOverrideEntity> overrides = fxRateOverridesRepository.findAll();
    return mapEntitiesToDtos(overrides);
  }

  @Override
  public Set<FxRateOverride> saveAllOverrides(Set<FxRateOverride> overrides) {
    Objects.requireNonNull(overrides);
    Iterable<FxRateOverrideEntity> savedEntities = fxRateOverridesRepository
        .saveAll(overrides.stream().map(f -> new FxRateOverrideEntity(f)).collect(Collectors.toSet()));
    return mapEntitiesToDtos(savedEntities);
  }

  private Set<FxRateOverride> mapEntitiesToDtos(Iterable<FxRateOverrideEntity> overrides) {
    return StreamSupport.stream(overrides.spliterator(), false)
        .map(f -> conversionService.convert(f, FxRateOverride.class))
        .collect(Collectors.toSet());
  }
}
