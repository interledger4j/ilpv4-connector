package com.sappenin.ilpv4.settings;

import com.sappenin.ilpv4.model.settings.ConnectorSettings;

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
