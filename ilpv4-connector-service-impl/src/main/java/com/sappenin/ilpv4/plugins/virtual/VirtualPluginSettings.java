package com.sappenin.ilpv4.plugins.virtual;

import org.immutables.value.Value.Immutable;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;

import java.math.BigInteger;

/**
 * Configuration information relating to a {@link Plugin}.
 */
public interface VirtualPluginSettings extends PluginSettings {

  /**
   * highest balance allowed
   */
  BigInteger getMaxBalance();

  /**
   * prefix for ilp address
   */
  InterledgerAddressPrefix getPrefix();

  /**
   * This plugin's secret. The peer must know the corresponding public key.
   */
  String getSecret();

  /**
   * base64url encoded public key of this plugin's peer.
   */
  String getPeerPublicKey();

  /**
   * Used to communicate with this plugin's peer.  For example, https://example.com/rpc.
   */
  String getRpcUri();


  @Immutable
  abstract class AbstractVirtualPluginSettings implements VirtualPluginSettings {

  }

}
