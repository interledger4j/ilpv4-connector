package org.interledger.connector.settings;

import org.immutables.value.Value;

/**
 * FIXME
 */
@Value.Immutable
public interface ConnectorKey {

  static ImmutableConnectorKey.Builder builder() {
    return ImmutableConnectorKey.builder();
  }

  String alias();

  String version();

}
