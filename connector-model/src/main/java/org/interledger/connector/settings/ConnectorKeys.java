package org.interledger.connector.settings;


import org.immutables.value.Value;

/**
 * Named keys that are configured for use by the connector.
 */
@Value.Immutable
public interface ConnectorKeys {

  static ImmutableConnectorKeys.Builder builder() {
    return ImmutableConnectorKeys.builder();
  }

  /**
   * Core secret for the connector
   *
   * @return key
   */
  ConnectorKey secret0();

  /**
   * Key for incoming/outgoing shared secrets on account settings
   * @return key
   */
  ConnectorKey accountSettings();

}
