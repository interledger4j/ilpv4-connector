package com.sappenin.interledger.ilpv4.connector.routing;

/**
 * Used for routing internal packets
 *
 * @deprecated This class is unused, and will go away in a future release. "Internal" and "External" routing decisions
 * should be made based upon the ILP address scheme.
 */
@Deprecated
public interface InternalRoutingService extends PaymentRouter<Route> {

  Route addRoute(Route internalRoute);

  /**
   * Accessor for the underlying {@link RoutingTable} used by this payment router.
   */
  RoutingTable<Route> getRoutingTable();
}
