package org.interledger.connector.routing;

import java.util.List;

/**
 * Defines a centralized service that manages incoming and outgoing <tt>external</tt> (i.e., shared with ILP nodes
 * outside of this Connector) routing updates. Also manages the primary routing table for a Connector. Note that this
 * service is meant for externally-facing routing decisions only.
 */
public interface ExternalRoutingService extends PaymentRouter<Route>, StaticRoutesManager {

  /**
   * Start the Routing Service, re-initializing it if necessary.
   */
  void start();

  /**
   *
   * @return all routes known by the connector
   */
  List<Route> getAllRoutes();

}
