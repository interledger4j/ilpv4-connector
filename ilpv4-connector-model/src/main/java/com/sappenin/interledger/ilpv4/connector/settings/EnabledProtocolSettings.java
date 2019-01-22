package com.sappenin.interledger.ilpv4.connector.settings;

import org.immutables.value.Value;

/**
 * Indicates which protocols should be enabled or disabled in this Connector.
 */
public interface EnabledProtocolSettings {

  static ImmutableEnabledProtocolSettings.Builder builder() {
    return ImmutableEnabledProtocolSettings.builder();
  }

  /**
   * Whether this Connector can handle `PING` packets per RFC-TODO.
   *
   * @see "RFC-LINK"
   */
  default boolean isPingProtocolEnabled() {
    return true;
  }

  /**
   * Whether this Connector can handle `ECHO` packets per RFC-TODO.
   *
   * @see "RFC-LINK"
   */
  default boolean isEchoProtocolEnabled() {
    return false;
  }

  @Value.Immutable(intern = true)
  abstract class AbstractEnabledProtocolSettings implements EnabledProtocolSettings {

    @Override
    @Value.Default
    public boolean isPingProtocolEnabled() {
      return true;
    }

    @Override
    @Value.Default
    public boolean isEchoProtocolEnabled() {
      return false;
    }


  }

}