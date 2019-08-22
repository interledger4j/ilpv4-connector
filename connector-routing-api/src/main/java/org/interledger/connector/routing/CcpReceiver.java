package org.interledger.connector.routing;

import org.interledger.connector.ccp.CcpRouteControlRequest;
import org.interledger.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Defines a receiver of Route Broadcast Protocol (RBP), formerly known as Connector-to-Connector Protocol or `CCP`.
 *
 * @see "https://github.com/interledger/rfcs/pull/455"
 */
public interface CcpReceiver {

  /**
   * Receive and process information from a remote peer about new routes that the peer would like us to know about.
   * Return the prefixes of any changed routes.
   *
   * @param routeUpdateRequest An instance of {@link CcpRouteControlRequest}.
   *
   * @return A {@link List} of prefixes whose routes have changed.
   */
  List<InterledgerAddressPrefix> handleRouteUpdateRequest(CcpRouteUpdateRequest routeUpdateRequest)
    throws InterledgerProtocolException;

  /**
   * Send a Route Control message to the remote peer to instruct it to begin sending route updates to us.
   *
   * @return An {@link InterledgerResponsePacket} that is the response from the remote peer for this route-control
   * request.
   */
  InterledgerResponsePacket sendRouteControl();

  /**
   * Perform the following action on each item in the routing table.
   *
   * @param action A {@link Consumer} to apply to each element in the route-update log.
   */
  void forEachIncomingRoute(final BiConsumer<InterledgerAddressPrefix, IncomingRoute> action);

  /**
   * Return the single route that corresponds to the supplied {@code prefix}.
   *
   * @param addressPrefix The {@link InterledgerAddressPrefix} to lookup an incoming route for.
   *
   * @return An optionally present {@link IncomingRoute}.
   */
  Optional<IncomingRoute> getIncomingRouteForPrefix(final InterledgerAddressPrefix addressPrefix);

}
