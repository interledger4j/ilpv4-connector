package com.sappenin.ilpv4.client;

import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;

/**
 * A client for communicating with an ILPv4 Service Provider.
 */
public interface IlpClient<T extends PluginSettings> extends Plugin<T> {

}
