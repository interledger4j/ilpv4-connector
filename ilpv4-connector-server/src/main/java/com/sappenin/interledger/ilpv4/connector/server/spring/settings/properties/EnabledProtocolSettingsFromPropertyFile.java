package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class EnabledProtocolSettingsFromPropertyFile implements EnabledProtocolSettings {

  private boolean pingProtocolEnabled;
  private boolean echoProtocolEnabled;

  @Override
  public boolean isPingProtocolEnabled() {
    return pingProtocolEnabled;
  }

  public void setPingProtocolEnabled(boolean pingProtocolEnabled) {
    this.pingProtocolEnabled = pingProtocolEnabled;
  }

  @Override
  public boolean isEchoProtocolEnabled() {
    return echoProtocolEnabled;
  }

  public void setEchoProtocolEnabled(boolean echoProtocolEnabled) {
    this.echoProtocolEnabled = echoProtocolEnabled;
  }
}
