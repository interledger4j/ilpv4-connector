package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.ilpv4.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerProtocolException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface CcpReceiver {

  /**
   * Receive and process information from a remote peer/account about new routes that the peer would like us to know
   * about. Return the prefixes of any changed routes.
   *
   * @param routeUpdateRequest An instance of {@link CcpRouteControlRequest}.
   *
   * @return A {@link List} of prefixes whose routes have changed.
   */
  CompletableFuture<List<InterledgerAddressPrefix>> handleRouteUpdateRequest(CcpRouteUpdateRequest routeUpdateRequest)
    throws InterledgerProtocolException;

  /**
   * Send a Route Control message to the remote peer to instruct it to begin sending getRoute updates to us.
   *
   * @return A completable future containing an instance of {@link InterledgerFulfillment}, that is the expected
   * success-response from the remote peer.
   */
  CompletableFuture<InterledgerFulfillPacket> sendRouteControl();

  /**
   * Perform the following action on each item in the routing table.
   *
   * @param action A {@link Consumer} to apply to each element in the getRoute-update log.
   */
  void forEachIncomingRoute(final Consumer<IncomingRoute> action);

  /**
   * Obtain an iterable for all incoming routes.
   *
   * @return
   */
  Iterable<IncomingRoute> getAllIncomingRoutes();

  /**
   * Return the single getRoute that corresponds to the supplied {@code prefix}.
   *
   * @param addressPrefix
   *
   * @return
   */
  Optional<IncomingRoute> getRouteForPrefix(final InterledgerAddressPrefix addressPrefix);

}
