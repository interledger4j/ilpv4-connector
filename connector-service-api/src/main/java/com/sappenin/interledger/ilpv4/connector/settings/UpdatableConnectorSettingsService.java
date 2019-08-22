package com.sappenin.interledger.ilpv4.connector.settings;

import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;

/**
 * Provides runtime-reloadable access to all settings for this connector (peers, routing, etc).
 */
public interface UpdatableConnectorSettingsService extends ConnectorSettingsService {

  /**
   * Update the connector settings for this connector.
   *
   * @param connectorSettings
   */
  void setConnectorSettings(final ConnectorSettings connectorSettings);
}
