package org.interledger.connector.routing;

/**
 * Defines a centralized service that manages incoming and outgoing <tt>external</tt> (i.e., shared with ILP nodes
 * outside of this Connector) routing updates. Also manages the primary routing table for a Connector. Note that this
 * service is meant for externally-facing routing decisions only.
 */
public interface ExternalRoutingService extends PaymentRouter<Route> {

  /**
   * Start the Routing Service, re-initializing it if necessary.
   */
  void start();

  //  /**
  //   * Accessor for the underlying {@link RoutingTable} used by this payment router.
  //   */
  //  public RoutingTable<Route> getRoutingTable();
}
