package org.interledger.connector.server.spring.settings.properties;

import org.interledger.connector.settings.FxConnectionSettings;
import org.interledger.connector.settings.FxSettings;

public class FxSettingsFromPropertyFile implements FxSettings {

  FxConnectionSettingsFromPropertyFile connectionDefaults = new FxConnectionSettingsFromPropertyFile();

  @Override
  public FxConnectionSettings connectionDefaults() {
    return connectionDefaults;
  }

  public void setConnectionDefaults(FxConnectionSettingsFromPropertyFile connectionDefaults) {
    this.connectionDefaults = connectionDefaults;
  }
}
