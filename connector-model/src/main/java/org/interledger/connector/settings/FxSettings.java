package org.interledger.connector.settings;

import org.immutables.value.Value;

/**
 * A wrapper class for {@link FxConnectionSettings} to conform to the existing property hierarchy
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface FxSettings {

  static ImmutableFxSettings.Builder builder() {
    return ImmutableFxSettings.builder();
  }

  @Value.Default
  default FxConnectionSettings connectionDefaults() {
    return FxConnectionSettings.builder().build();
  }
}
