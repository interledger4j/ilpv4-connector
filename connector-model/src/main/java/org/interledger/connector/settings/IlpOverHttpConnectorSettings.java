package org.interledger.connector.settings;

import org.immutables.value.Value;

@Value.Immutable(intern = true)
@Value.Modifiable
public interface IlpOverHttpConnectorSettings {

  static ImmutableIlpOverHttpConnectorSettings.Builder builder() {
    return ImmutableIlpOverHttpConnectorSettings.builder();
  }

  @Value.Default
  default IlpOverHttpConnectionSettings connectionDefaults() {
    return IlpOverHttpConnectionSettings.builder().build();
  }

}
