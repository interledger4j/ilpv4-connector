package com.sappenin.ilpv4.connector.ccp;

import com.sappenin.ilpv4.model.RoutingTableId;
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
   * The routing table ID that the requesting node knows about when this request is issued.
   *
   * @return A {@link UUID}.
   */
  RoutingTableId routingTableId();

  /**
   * The current getEpoch index that the requesting node has for the specified routing table identifier.
   *
   * @return A <tt>long</tt>.
   */
  int currentEpochIndex();

  /**
   * The getEpoch index that the log of this update request starts from.
   *
   * @return A <tt>long</tt>.
   */
  int fromEpochIndex();

  /**
   * The getEpoch index that the log of this update request ends at.
   *
   * @return A <tt>long</tt>.
   */
  int toEpochIndex();

  /**
   * Reserved for the future, currently not used.
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
