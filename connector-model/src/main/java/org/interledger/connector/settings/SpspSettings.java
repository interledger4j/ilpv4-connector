package org.interledger.connector.settings;

import org.immutables.value.Value;

import java.util.Optional;

/**
 * Defines connector-wide settings for SPSP.
 */
@Value.Immutable
public interface SpspSettings {

  static ImmutableSpspSettings.Builder builder() {
    return ImmutableSpspSettings.builder();
  }

  /**
   * An ILP address prefix such that any packet destined for {operator-address}.{addressPrefixSegment} will be treated
   * as a locally terminated STREAM packet, and not forwarded to a remote link.
   *
   * @return A {@link String} containing an ILP address prefix; default is {@code spsp}.
   */
  default String addressPrefixSegment() {
    return "spsp";
  }

  /**
   * An optional URL path that the Connector should respond to SPSP requests on. Default is empty, meaning GET requests
   * to https://connector.example.com/bob would be the default SPSP URL.
   *
   * @return An optionally-present String.
   */
  Optional<String> urlPathPrefix();

}
