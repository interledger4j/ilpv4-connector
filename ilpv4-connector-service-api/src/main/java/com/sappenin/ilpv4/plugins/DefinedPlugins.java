package com.sappenin.ilpv4.plugins;

import org.interledger.plugin.lpiv2.PluginType;
import org.interledger.plugin.lpiv2.SimulatedChildPlugin;

public class DefinedPlugins {

  public static PluginType SIMULATED_CHILD_PLUGIN_TYPE = SimulatedChildPlugin.PLUGIN_TYPE;
  public static PluginType BTP_CHILD_PLUGIN_TYPE = PluginType.of("BtpChildPlugin");

}