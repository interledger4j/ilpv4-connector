package com.sappenin.ilpv4.model;

import java.util.Objects;

/**
 * Plugin types supported by this Connector.
 */
public enum PluginType {
  MOCK("Mock Plugin"),
  BTP("BTP Plugin");

  private final String pluginDescription;

  PluginType(final String pluginDescription) {
    this.pluginDescription = Objects.requireNonNull(pluginDescription);
  }

  public String getPluginDescription() {
    return pluginDescription;
  }
}
