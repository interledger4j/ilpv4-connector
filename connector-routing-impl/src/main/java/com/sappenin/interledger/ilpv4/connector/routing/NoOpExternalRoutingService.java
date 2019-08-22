package com.sappenin.interledger.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;

import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link ExternalRoutingService} that neither responds to nor issues routing requests, and assumes
 * only a single account exists for purposes of routing.
 */
public class NoOpExternalRoutingService implements ExternalRoutingService {

  private final RoutingTable<Route> routeRoutingTable;

  /**
   * Required-args Constructor.
   */
  public NoOpExternalRoutingService() {
    this.routeRoutingTable = new InMemoryRoutingTable();
  }

  @Override
  public void start() {
    this.routeRoutingTable.reset();
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);
    return this.routeRoutingTable.findNextHopRoute(finalDestinationAddress);
  }

//  /**
//   * Accessor for the underlying {@link RoutingTable} used by this payment router.
//   */
//  @Override
//  public RoutingTable<Route> getRoutingTable() {
//    return this.routeRoutingTable;
//  }
}
