package com.sappenin.interledger.ilpv4.connector.routing;

import org.immutables.value.Value;
import org.interledger.support.immutables.Wrapped;
import org.interledger.support.immutables.Wrapper;

import java.util.UUID;

/**
 * Typed-identifiers for all routing components.
 */
public interface Ids {

  /**
   * Identifier for {@link RoutingTable}.
   */
  @Value.Immutable
  @Wrapped
  abstract class _RoutingTableId extends Wrapper<UUID> {

    @Override
    public String toString() {
      return this.value().toString();
    }
  }
}