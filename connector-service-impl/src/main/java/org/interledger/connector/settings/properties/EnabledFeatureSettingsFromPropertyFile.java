package org.interledger.connector.settings.properties;

import org.interledger.connector.settings.EnabledFeatureSettings;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class EnabledFeatureSettingsFromPropertyFile implements EnabledFeatureSettings {

  private boolean rateLimitingEnabled;
  private boolean require32ByteSharedSecrets;

  @Override
  public boolean isRateLimitingEnabled() {
    return rateLimitingEnabled;
  }

  public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
    this.rateLimitingEnabled = rateLimitingEnabled;
  }

  @Override
  public boolean isRequire32ByteSharedSecrets() {
    return require32ByteSharedSecrets;
  }

  public void setRequire32ByteSharedSecrets(boolean require32ByteSharedSecrets) {
    this.require32ByteSharedSecrets = require32ByteSharedSecrets;
  }
}
