package com.sappenin.ilpv4.connector.ccp;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * An object that represents a request to update one or more routes.
 */
@Value.Immutable
public interface CcpRouteUpdateRequest {

  /**
   * The unique identifier of an entry in the routing table.
   *
   * @return A {@link UUID} representing a route.
   */
  UUID routingTableId();

  long currentEpochIndex();

  long fromEpochIndex();

  long toEpochIndex();

  long holdDownTime();

  InterledgerAddress speaker();

  /**
   * New Routes that the speaker is advertising to a node.
   *
   * @return A {@link Collection} of routes of type {@link CcpRoute}.
   */
  @Value.Default
  default Collection<CcpRoute> newRoutes() {
    return Collections.emptyList();
  }

  /**
   * Withdawn Routes that the speaker is advertising as being no longer valid or useful.
   *
   * @return A {@link Collection} of route-identifiers of type {@link InterledgerAddress}.
   */
  @Value.Default
  default Collection<CcpWithdrawnRoute> withdrawnRoutePrefixes() {
    return Collections.emptyList();
  }
}
