package com.sappenin.interledger.ilpv4.connector.settings;

import org.immutables.value.Value;

/**
 * Indicates which connector features are enabled or disabled.
 */
public interface EnabledFeatureSettings {

  static ImmutableEnabledFeatureSettings.Builder builder() {
    return ImmutableEnabledFeatureSettings.builder();
  }

  /**
   * Whether this Connector rate-limits accounts.
   */
  default boolean isRateLimitingEnabled() {
    return false;
  }

  @Value.Immutable(intern = true)
  abstract class AbstractEnabledFeatureSettings implements EnabledFeatureSettings {

    @Override
    @Value.Default
    public boolean isRateLimitingEnabled() {
      return true;
    }

  }

}