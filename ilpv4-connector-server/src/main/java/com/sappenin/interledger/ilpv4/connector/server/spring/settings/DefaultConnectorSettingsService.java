package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.UpdatableConnectorSettingsService;

import java.util.Objects;

/**
 * A default implementation of {@link UpdatableConnectorSettingsService}.
 */
public class DefaultConnectorSettingsService implements UpdatableConnectorSettingsService {

  // This is interned...
  private ImmutableConnectorSettings connectorSettings;

  /**
   * Accessor for a view of the current settings configured for this Connector.
   *
   * @return An immutable instance of {@link ConnectorSettings}.
   */
  @Override
  public ConnectorSettings getConnectorSettings() {
    return this.connectorSettings;
  }

  /**
   * Accessor for a view of the current settings configured for this Connector.
   *
   * @param connectorSettings
   *
   * @return An immutable instance of {@link ConnectorSettings}.
   */
  @Override
  public void setConnectorSettings(final ConnectorSettings connectorSettings) {
    Objects.requireNonNull(connectorSettings);
    this.connectorSettings = ImmutableConnectorSettings.copyOf(connectorSettings);
  }
}
