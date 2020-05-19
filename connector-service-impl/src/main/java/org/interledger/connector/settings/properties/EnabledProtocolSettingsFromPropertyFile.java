package org.interledger.connector.settings.properties;

import org.interledger.connector.settings.EnabledProtocolSettings;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class EnabledProtocolSettingsFromPropertyFile implements EnabledProtocolSettings {

  private boolean pingProtocolEnabled;
  private boolean peerRoutingEnabled;
  private boolean ildcpEnabled;
  private boolean spspEnabled;
  private boolean openPaymentsEnabled;

  @Override
  public boolean isPingProtocolEnabled() {
    return pingProtocolEnabled;
  }

  public void setPingProtocolEnabled(boolean pingProtocolEnabled) {
    this.pingProtocolEnabled = pingProtocolEnabled;
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

  @Override
  public boolean isSpspEnabled() {
    return spspEnabled;
  }

  public void setSpspEnabled(boolean spspEnabled) {
    this.spspEnabled = spspEnabled;
  }

  @Override
  public boolean isOpenPaymentsEnabled() {
    return openPaymentsEnabled;
  }

  public void setOpenPaymentsEnabled(boolean openPaymentsEnabled) {
    this.openPaymentsEnabled = openPaymentsEnabled;
  }
}
