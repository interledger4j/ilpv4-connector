package com.sappenin.interledger.ilpv4.connector.ccp;

import com.sappenin.interledger.ilpv4.connector.routing.RoutingTableId;
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

  static ImmutableCcpRouteUpdateRequest.Builder builder() {
    return ImmutableCcpRouteUpdateRequest.builder();
  }

  /**
   * The routing table ID that the requesting node knows about when this request is issued.
   *
   * @return A {@link UUID}.
   */
  RoutingTableId routingTableId();

  /**
   * The current epoch index that the requesting node has for the specified routing table identifier.
   *
   * @return A <tt>long</tt>.
   */
  int currentEpochIndex();

  /**
   * The epoch index that the log of this update request starts from.
   *
   * @return A <tt>long</tt>.
   */
  int fromEpochIndex();

  /**
   * The epoch index that the log of this update request ends at.
   *
   * @return A <tt>long</tt>.
   */
  int toEpochIndex();

  /**
   * Time in milliseconds for which the sending connector claims its routes to be fresh, without another heartbeat.
   *
   * @return
   */
  long holdDownTime();

  /**
   * The {@link InterledgerAddress} of the Node making this Route Update request.
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress speaker();

  /**
   * New Routes that the speaker is advertising to the counterpart node.
   *
   * @return A {@link Collection} of routes of type {@link CcpNewRoute}.
   */
  @Value.Default
  default Collection<CcpNewRoute> newRoutes() {
    return Collections.emptyList();
  }

  /**
   * Withdrawn Routes that the speaker is advertising as being no longer valid or useful.
   *
   * @return A {@link Collection} of getRoute-identifiers of type {@link InterledgerAddress}.
   */
  @Value.Default
  default Collection<CcpWithdrawnRoute> withdrawnRoutePrefixes() {
    return Collections.emptyList();
  }
}
