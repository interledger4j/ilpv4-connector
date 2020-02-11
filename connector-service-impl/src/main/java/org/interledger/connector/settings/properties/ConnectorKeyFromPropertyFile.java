package org.interledger.connector.settings.properties;

import org.interledger.crypto.CryptoKey;

import java.util.Objects;

public class ConnectorKeyFromPropertyFile implements CryptoKey {

  private String alias;

  private String version;

  private String keyringId;

  public String alias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String version() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String keyringId() {
    return keyringId;
  }

  public void setKeyringId(String keyringId) {
    this.keyringId = keyringId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof CryptoKey)) {
      return false;
    }
    CryptoKey that = (CryptoKey) o;
    return alias.equals(that.alias()) &&
      version.equals(that.version());
  }

  @Override
  public int hashCode() {
    return Objects.hash(alias, version);
  }

  @Override
  public String toString() {
    return "ConnectorKeyFromPropertyFile{" +
      "alias='" + alias + '\'' +
      ", version='" + version + '\'' +
      '}';
  }
}
