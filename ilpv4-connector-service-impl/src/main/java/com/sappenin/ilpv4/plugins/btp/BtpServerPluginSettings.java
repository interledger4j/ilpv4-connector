package com.sappenin.ilpv4.plugins.btp;

import org.immutables.value.Value;

public interface BtpServerPluginSettings extends BtpPluginSettings {

  static ImmutableBtpServerPluginSettings.Builder builder() {
    return ImmutableBtpServerPluginSettings.builder();
  }

  @Value.Immutable
  abstract class AbstractBtpServerPluginSettings implements BtpServerPluginSettings {

  }

}
