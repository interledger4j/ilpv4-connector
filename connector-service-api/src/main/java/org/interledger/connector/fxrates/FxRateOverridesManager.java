package org.interledger.connector.fxrates;

import java.util.Set;

public interface FxRateOverridesManager {

  Set<FxRateOverride> getAllOverrides();

  Set<FxRateOverride> saveAllOverrides(Set<FxRateOverride> overrides);
}
