package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;
import com.sappenin.ilpv4.connector.ccp.CcpRouteUpdateRequest;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Models a route update, as received from a remote peer. This update is keyed by {@link #prefix()}, and  Information
 * about route updates is transmitted via CCP, and then synthesized into this object type so that future routing table
 * entries might have more information than strictly allowed by the structure of {@link CcpRouteUpdateRequest}.
 */
@Value.Immutable
public interface RouteUpdate {

  /**
   * The ILP address prefix that this route update applies to.
   */
  InterledgerAddressPrefix prefix();

  /**
   * The epoch index for this route update.¬
   *
   * @return
   */
  long epoch();

  /**
   * An entry in a Routing Table that specifies both a next-hop destination, as well as an overall path, for sending a
   * packet to a final destination address.
   */
  Optional<RoutingTableEntry> route();

  @Value.Derived
  default RouteUpdate withoutRoutes() {
    return ImmutableRouteUpdate.builder()
      .prefix(prefix())
      .epoch(epoch())
      .build();
  }
}
