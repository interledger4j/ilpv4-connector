package org.interledger.connector.server.spring.settings.properties;

import org.interledger.connector.accounts.AccountRateLimitSettings;

import java.util.Optional;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class AccountRateLimitSettingsFromPropertyFile implements AccountRateLimitSettings {

  private Optional<Integer> maxPacketsPerSecond = Optional.empty();

  @Override
  public Optional<Integer> getMaxPacketsPerSecond() {
    return maxPacketsPerSecond;
  }

  public void setMaxPacketsPerSecond(Optional<Integer> packetsPerSecond) {
    this.maxPacketsPerSecond = packetsPerSecond;
  }
}
