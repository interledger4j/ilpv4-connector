package org.interledger.connector.server.spring.settings.properties;


import org.interledger.connector.settings.ConnectorKeys;

import java.util.Objects;

public class ConnectorKeysFromPropertyFile implements ConnectorKeys {

  private ConnectorKeyFromPropertyFile secret0;

  private ConnectorKeyFromPropertyFile accountSettings;

  public ConnectorKeyFromPropertyFile secret0() {
    return secret0;
  }

  public void setSecret0(ConnectorKeyFromPropertyFile secret0) {
    this.secret0 = secret0;
  }

  public ConnectorKeyFromPropertyFile accountSettings() {
    return accountSettings;
  }

  public void setAccountSettings(ConnectorKeyFromPropertyFile accountSettings) {
    this.accountSettings = accountSettings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConnectorKeys)) {
      return false;
    }
    ConnectorKeys that = (ConnectorKeys) o;
    return secret0.equals(that.secret0()) &&
      accountSettings.equals(that.accountSettings());
  }

  @Override
  public int hashCode() {
    return Objects.hash(secret0, accountSettings);
  }

  @Override
  public String toString() {
    return "ConnectorKeysFromPropertyFile{" +
      "secret0=" + secret0 +
      ", accountSettings=" + accountSettings +
      '}';
  }
}
