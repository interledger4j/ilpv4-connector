package org.interledger.connector.settings;


import org.immutables.value.Value;

@Value.Immutable
public interface ConnectorKeys {

  static ImmutableConnectorKeys.Builder builder() {
    return ImmutableConnectorKeys.builder();
  }

  ConnectorKey secret0();

  ConnectorKey accountSettings();

  @Value.Default
  default boolean require32Bytes() {
    return true;
  }

}
