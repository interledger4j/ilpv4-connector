package com.sappenin.interledger.ilpv4.connector.routing;

/**
 * Used for routing internalpackets
 */
public interface InternalRoutingService extends PaymentRouter<Route> {

  Route addRoute(Route internalRoute);

  /**
   * Accessor for the underlying {@link RoutingTable} used by this payment router.
   */
  RoutingTable<Route> getRoutingTable();
}
