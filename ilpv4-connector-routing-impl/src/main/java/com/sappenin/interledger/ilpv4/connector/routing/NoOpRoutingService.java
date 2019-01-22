package com.sappenin.interledger.ilpv4.connector.routing;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.core.InterledgerAddress;

import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link RoutingService} that neither responds to nor issues routing requests, and assumes only a
 * single account exists for purposes of routing.
 */
public class NoOpRoutingService implements RoutingService {

  private final RoutingTable<Route> routeRoutingTable;

  /**
   * Required-args Constructor.
   */
  public NoOpRoutingService() {
    this.routeRoutingTable = new InMemoryRoutingTable();
  }

  @Override
  public void start() {
    this.routeRoutingTable.reset();
  }

  /**
   * Shutdown the Routing Service.
   */
  @Override
  public void shutdown() {
    // Not technically necessary, but just in-case...
    this.routeRoutingTable.reset();
  }

  @Override
  public void registerAccount(AccountId accountId) {
    // No-op. Routing is disabled in this implementation, so no need to track any accounts.
  }

  @Override
  public void unregisterAccount(AccountId accountId) {
    // No-op. Routing is disabled in this implementation, so no need to track any accounts.
  }

  @Override
  public void setTrackedAccount(RoutableAccount routableAccount) {
    // No-op. Routing is disabled in this implementation, so no need to track any accounts.
  }

  @Override
  public Optional<RoutableAccount> getTrackedAccount(AccountId accountId) {
    // Routing is disabled in this implementation, so no tracked accounts exist.
    return Optional.empty();
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);
    return this.routeRoutingTable.findNextHopRoute(finalDestinationAddress);
  }

  /**
   * Accessor for the underlying {@link RoutingTable} used by this payment router.
   */
  @Override
  public RoutingTable<Route> getRoutingTable() {
    return this.routeRoutingTable;
  }
}
