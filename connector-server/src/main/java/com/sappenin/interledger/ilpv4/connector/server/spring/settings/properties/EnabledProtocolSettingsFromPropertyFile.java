package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class EnabledProtocolSettingsFromPropertyFile implements EnabledProtocolSettings {

  private boolean pingProtocolEnabled;
  private boolean peerConfigEnabled;
  private boolean peerRoutingEnabled;
  private boolean ildcpEnabled;

  @Override
  public boolean isPingProtocolEnabled() {
    return pingProtocolEnabled;
  }

  public void setPingProtocolEnabled(boolean pingProtocolEnabled) {
    this.pingProtocolEnabled = pingProtocolEnabled;
  }

  @Override
  public boolean isPeerConfigEnabled() {
    return peerConfigEnabled;
  }

  public void setPeerConfigEnabled(boolean peerConfigEnabled) {
    this.peerConfigEnabled = peerConfigEnabled;
  }

  @Override
  public boolean isPeerRoutingEnabled() {
    return peerRoutingEnabled;
  }

  public void setPeerRoutingEnabled(boolean peerRoutingEnabled) {
    this.peerRoutingEnabled = peerRoutingEnabled;
  }

  @Override
  public boolean isIldcpEnabled() {
    return ildcpEnabled;
  }

  public void setIldcpEnabled(boolean ildcpEnabled) {
    this.ildcpEnabled = ildcpEnabled;
  }
}
