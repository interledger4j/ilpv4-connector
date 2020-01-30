package org.interledger.connector.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link EnabledFeatureSettings}.
 */
public class EnabledFeatureSettingsTest {

  @Test
  public void builderWithDefaults() {
    final EnabledFeatureSettings enabledFeatureSettings = EnabledFeatureSettings.builder().build();
    assertThat(enabledFeatureSettings.isRateLimitingEnabled()).isFalse();
    assertThat(enabledFeatureSettings.isLocalSpspFulfillmentEnabled()).isFalse();
  }

  @Test
  public void testInterfaceDefaults() {
    final EnabledFeatureSettings enabledFeatureSettings = new EnabledFeatureSettings() {
    };
    assertThat(enabledFeatureSettings.isRateLimitingEnabled()).isFalse();
    assertThat(enabledFeatureSettings.isLocalSpspFulfillmentEnabled()).isFalse();
  }

}
