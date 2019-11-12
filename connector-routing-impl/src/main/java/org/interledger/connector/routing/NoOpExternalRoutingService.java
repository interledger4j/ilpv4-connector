package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
  public Collection<Route> getAllRoutes() {
    Set<Route> allRoutes = new HashSet<>();
    routeRoutingTable.forEach((prefix, route) -> allRoutes.add(route));
    return allRoutes;
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);
    return this.routeRoutingTable.findNextHopRoute(finalDestinationAddress);
  }

  @Override
  public Set<StaticRoute> getAllStaticRoutes() {
    return Collections.emptySet();
  }

  @Override
  public void deleteStaticRouteByPrefix(InterledgerAddressPrefix prefix) {
    this.routeRoutingTable.removeRoute(prefix);
  }

  @Override
  public StaticRoute updateStaticRoute(StaticRoute route) {
    return null;
  }
}
