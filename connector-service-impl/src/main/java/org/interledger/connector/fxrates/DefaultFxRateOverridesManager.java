package org.interledger.connector.fxrates;

import org.interledger.connector.persistence.repositories.FxRateOverridesRepository;

import java.util.Objects;
import java.util.Set;

public class DefaultFxRateOverridesManager implements FxRateOverridesManager {

  private final FxRateOverridesRepository repository;

  public DefaultFxRateOverridesManager(FxRateOverridesRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  @Override
  public Set<FxRateOverride> getAllOverrides() {
    return repository.getAllOverrides();
  }

  @Override
  public Set<FxRateOverride> saveAllOverrides(Set<FxRateOverride> overrides) {
    Objects.requireNonNull(overrides);
    return repository.saveAllOverrides(overrides);
  }
}
