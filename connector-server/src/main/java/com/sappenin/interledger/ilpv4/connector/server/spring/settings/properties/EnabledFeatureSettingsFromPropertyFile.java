package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.sappenin.interledger.ilpv4.connector.settings.EnabledFeatureSettings;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class EnabledFeatureSettingsFromPropertyFile implements EnabledFeatureSettings {

  private boolean rateLimitingEnabled;

  @Override
  public boolean isRateLimitingEnabled() {
    return rateLimitingEnabled;
  }

  public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
    this.rateLimitingEnabled = rateLimitingEnabled;
  }
}
