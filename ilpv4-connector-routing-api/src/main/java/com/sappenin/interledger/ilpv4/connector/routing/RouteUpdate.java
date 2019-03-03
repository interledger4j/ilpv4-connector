package com.sappenin.interledger.ilpv4.connector.routing;

import org.immutables.value.Value;

import java.util.Optional;

/**
 * Models a getRoute update, as received from a remote peer. This update is keyed by {@link #getRoutePrefix()}, and is
 * typically transmitted via Connector-to-Connector Protocol (CCP).
 */
@Value.Immutable
public interface RouteUpdate extends BaseRoute {

  /**
   * The getEpoch index for this getRoute update.¬
   *
   * @return
   */
  int getEpoch();

  /**
   * An entry in a Routing Table that specifies both a next-hop destination, as well as an overall path, for sending a
   * packet to a final destination address.
   */
  Optional<Route> getRoute();
}