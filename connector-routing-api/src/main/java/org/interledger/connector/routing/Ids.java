package org.interledger.connector.routing;

import org.immutables.value.Value;
import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;

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
