package org.interledger.connector.settings.properties;

import org.interledger.connector.settings.SpspSettings;

import java.util.Optional;

public class SpspSettingsFromPropertyFile implements SpspSettings {

  private String addressPrefixSegment;

  private String urlPath;

  @Override
  public String addressPrefixSegment() {
    return addressPrefixSegment;
  }

  public void setAddressPrefixSegment(String addressPrefixSegment) {
    this.addressPrefixSegment = addressPrefixSegment;
  }

  @Override
  public Optional<String> urlPath() {
    return Optional.ofNullable(urlPath);
  }

  public void setUrlPath(String urlPath) {
    this.urlPath = urlPath;
  }

}
