package com.sappenin.ilpv4.connector.routing;

import java.time.Duration;

/**
 * Settings for route-broadcasting, generally set on a per-peer basis. This class models the YAML format for spring-boot
 * automatic configuration property loading.
 */
public class InitialRouteBroadcastSettings {

  private boolean routeBroadcastEnabled;

  private Duration routeBroadcastInterval;

  public boolean isRouteBroadcastEnabled() {
    return routeBroadcastEnabled;
  }

  public void setRouteBroadcastEnabled(boolean routeBroadcastEnabled) {
    this.routeBroadcastEnabled = routeBroadcastEnabled;
  }

  public Duration getRouteBroadcastInterval() {
    return routeBroadcastInterval;
  }

  public void setRouteBroadcastInterval(Duration routeBroadcastInterval) {
    this.routeBroadcastInterval = routeBroadcastInterval;
  }
}
