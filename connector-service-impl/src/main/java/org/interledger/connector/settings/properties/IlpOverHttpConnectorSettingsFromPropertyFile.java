package org.interledger.connector.settings.properties;

import org.interledger.connector.settings.IlpOverHttpConnectionSettings;
import org.interledger.connector.settings.IlpOverHttpConnectorSettings;

public class IlpOverHttpConnectorSettingsFromPropertyFile implements IlpOverHttpConnectorSettings {

  private IlpOverHttpConnectionSettingsFromPropertyFile connectionDefaults =
    new IlpOverHttpConnectionSettingsFromPropertyFile();

  @Override
  public IlpOverHttpConnectionSettings connectionDefaults() {
    return connectionDefaults;
  }

  public void setConnectionDefaults(IlpOverHttpConnectionSettingsFromPropertyFile connectionDefaults) {
    this.connectionDefaults = connectionDefaults;
  }

  public IlpOverHttpConnectionSettingsFromPropertyFile getConnectionDefaults() {
    return connectionDefaults;
  }
}
