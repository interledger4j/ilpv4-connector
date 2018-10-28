package com.sappenin.ilpv4.plugins.btp;

import org.immutables.value.Value;
import org.interledger.plugin.lpiv2.PluginSettings;

public interface BtpPluginSettings extends PluginSettings {

  /**
   * A typed key for the BTP Auth shared secret that both the client and server will use to authenticate a BTP session.
   */
  String KEY_SECRET = "secret";

  /**
   * The shared auth token, expected by a server or presented by a client, that both will use to authenticate a BTP
   * session.
   *
   * @return
   */
  String getSecret();

  @Value.Immutable
  abstract class AbstractBtpPluginSettings implements BtpPluginSettings {

  }
}
