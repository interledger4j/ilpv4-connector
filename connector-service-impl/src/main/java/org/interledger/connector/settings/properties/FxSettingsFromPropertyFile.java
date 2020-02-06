package org.interledger.connector.settings.properties;

import org.interledger.connector.settings.FxConnectionSettings;
import org.interledger.connector.settings.FxSettings;

public class FxSettingsFromPropertyFile implements FxSettings {

  private FxConnectionSettingsFromPropertyFile connectionDefaults = new FxConnectionSettingsFromPropertyFile();

  @Override
  public FxConnectionSettings connectionDefaults() {
    return connectionDefaults;
  }

  public void setConnectionDefaults(FxConnectionSettingsFromPropertyFile connectionDefaults) {
    this.connectionDefaults = connectionDefaults;
  }

  public FxConnectionSettingsFromPropertyFile getConnectionDefaults() {
    return connectionDefaults;
  }
}
