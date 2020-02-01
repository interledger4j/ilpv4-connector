package org.interledger.connector.server.spring.settings.properties;

import org.interledger.connector.settings.IlpOverHttpConnectionSettings;
import org.interledger.connector.settings.IlpOverHttpConnectorSettings;

public class IlpOverHttpConnectorSettingsFromPropertyFile implements IlpOverHttpConnectorSettings {

  IlpOverHttpConnectionSettingsFromPropertyFile connectionDefaults =
    new IlpOverHttpConnectionSettingsFromPropertyFile();

  @Override
  public IlpOverHttpConnectionSettings connectionDefaults() {
    return connectionDefaults;
  }

  public void setConnectionDefaults(IlpOverHttpConnectionSettingsFromPropertyFile connectionDefaults) {
    this.connectionDefaults = connectionDefaults;
  }
}
