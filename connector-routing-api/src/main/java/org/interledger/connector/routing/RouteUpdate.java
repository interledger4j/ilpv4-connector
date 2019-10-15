package org.interledger.connector.routing;

import org.immutables.value.Value;

import java.util.Optional;

/**
 * Models a route update, as received from a remote peer. This update is keyed by {@link #routePrefix()}, and is
 * typically transmitted via Connector-to-Connector Protocol (CCP).
 */
@Value.Immutable
public interface RouteUpdate extends BaseRoute {

  /**
   * The epoch index for this route update.¬
   *
   * @return
   */
  int epoch();

  /**
   * An entry in a Routing Table that specifies both a next-hop destination, as well as an overall path, for sending a
   * packet to a final destination address.
   */
  Optional<Route> route();
}
