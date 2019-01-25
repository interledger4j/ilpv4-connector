package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import org.interledger.core.InterledgerAddress;

import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link ExternalRoutingService} that manages an internal-only routing table used to route packets
 * that are only internally-routed by this Connector. These routes are not visible to external Connectors, do not
 * participate in CCP routing-table updates, and generally do not forward to an external address (e.g., `g.*`).
 */
public class DefaultInternalRoutingService implements InternalRoutingService {

  private final RoutingTable<Route> internalRoutingTable;

  /**
   * Required-args Constructor.
   */
  public DefaultInternalRoutingService() {
    this(new InMemoryRoutingTable());
  }

  /**
   * Required-args Constructor.
   */
  @VisibleForTesting
  protected DefaultInternalRoutingService(final RoutingTable<Route> internalRoutingTable) {
    this.internalRoutingTable = Objects.requireNonNull(internalRoutingTable);
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);
    return this.internalRoutingTable.findNextHopRoute(finalDestinationAddress);
  }

  @Override
  public Route addRoute(Route internalRoute) {
    return this.internalRoutingTable.addRoute(internalRoute);
  }

  /**
   * Accessor for the underlying {@link RoutingTable} used by this payment router.
   */
  @Override
  public RoutingTable<Route> getRoutingTable() {
    return this.getRoutingTable();
  }
}
