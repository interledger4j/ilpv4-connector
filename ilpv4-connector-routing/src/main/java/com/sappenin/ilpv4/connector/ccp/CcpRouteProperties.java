package com.sappenin.ilpv4.connector.ccp;

import org.immutables.value.Value;

@Value.Immutable
public interface CcpRouteProperties {

  String id();

  String value();

  @Value.Default
  default boolean isUtf8() {
    return false;
  }

  boolean optional();

  boolean transitive();

  /**
   * Determines if this route contains only partial information about the entire route.
   */
  boolean partial();

}
