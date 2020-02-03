package org.interledger.connector.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link EnabledProtocolSettings}.
 */
public class EnabledProtocolSettingsTest {

  @Test
  public void builderWithDefaults() {
    final EnabledProtocolSettings enabledProtocolSettings = EnabledProtocolSettings.builder().build();
    assertThat(enabledProtocolSettings.isPeerRoutingEnabled()).isTrue();
    assertThat(enabledProtocolSettings.isPingProtocolEnabled()).isTrue();
    assertThat(enabledProtocolSettings.isIldcpEnabled()).isTrue();
    assertThat(enabledProtocolSettings.isIlpOverHttpEnabled()).isTrue();
  }

  @Test
  public void testInterfaceDefaults() {
    final EnabledProtocolSettings enabledProtocolSettings = new EnabledProtocolSettings() {
    };
    assertThat(enabledProtocolSettings.isPeerRoutingEnabled()).isTrue();
    assertThat(enabledProtocolSettings.isPingProtocolEnabled()).isTrue();
    assertThat(enabledProtocolSettings.isIldcpEnabled()).isTrue();
    assertThat(enabledProtocolSettings.isIlpOverHttpEnabled()).isTrue();
  }
}
