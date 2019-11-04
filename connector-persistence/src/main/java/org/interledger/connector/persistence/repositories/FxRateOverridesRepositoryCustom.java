package org.interledger.connector.persistence.repositories;

import org.interledger.connector.fxrates.FxRateOverride;

import java.util.Set;

public interface FxRateOverridesRepositoryCustom {

  Set<FxRateOverride> getAllOverrides();

  Set<FxRateOverride> saveAllOverrides(Set<FxRateOverride> overrides);
}
