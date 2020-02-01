package org.interledger.connector.settings;

import org.immutables.value.Value;

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
