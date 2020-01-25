package org.interledger.connector.settings.properties;


import org.interledger.crypto.CryptoKeys;

import java.util.Objects;

public class ConnectorKeysFromPropertyFile implements CryptoKeys {

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
    if (!(o instanceof CryptoKeys)) {
      return false;
    }
    CryptoKeys that = (CryptoKeys) o;
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
