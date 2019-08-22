package org.interledger.connector.server.spring.settings;

import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.connector.settings.UpdatableConnectorSettingsService;

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
