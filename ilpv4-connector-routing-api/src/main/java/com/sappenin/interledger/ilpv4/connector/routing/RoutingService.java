package com.sappenin.interledger.ilpv4.connector.routing;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.plugin.lpiv2.Plugin;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines a centralized service that manages all routing decisions for an ILPv4 Connector.
 */
public interface RoutingService extends PaymentRouter<Route> {

  /**
   * Start the Routing Service, re-initializing it if necessary.
   */
  void start();

  /**
   * Shutdown the Routing Service.
   */
  void shutdown();

  /**
   * Register this service to respond to connect/disconnect events that may be emitted from a {@link Plugin}, and then
   * add the account to this service's internal machinery.
   *
   * @param account The address of a remote peer that can have packets routed to it.
   */
  void registerAccount(AccountId account);

  /**
   * Untrack a peer account address from participating in Routing.
   *
   * @param account The address of a remote peer that can have packets routed to it.
   */
  void unregisterAccount(AccountId account);

  void setTrackedAccount(RoutableAccount routableAccount);

  /**
   * Retrieve a routable account if it's being tracked.
   *
   * @param accountId The {@link AccountId} of the account to retried.
   *
   * @return An optionally-present {@link RoutableAccount}.
   */
  Optional<RoutableAccount> getTrackedAccount(AccountId accountId);

  /**
   * Sets the default route in the routing table for all prefixes.
   *
   * @param nextHopAccountId The {@link AccountId} of the account to use as a next-hop by default for any prefixes
   *                         supported by this Connector.
   */
  default void setDefaultRoute(final AccountId nextHopAccountId) {
    Objects.requireNonNull(nextHopAccountId);

    // Add a default global route for this account...
    final Route defaultGlobalRoute = ImmutableRoute.builder()
      .routePrefix(InterledgerAddressPrefix.GLOBAL)
      .expiresAt(Instant.MAX)
      .nextHopAccountId(nextHopAccountId)
      .build();
    this.getRoutingTable().addRoute(defaultGlobalRoute);

    final Route defaultTestRoute =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST).build();
    this.getRoutingTable().addRoute(defaultTestRoute);

    final Route defaultTest1Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST1).build();
    this.getRoutingTable().addRoute(defaultTest1Route);

    final Route defaultTest2Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST2).build();
    this.getRoutingTable().addRoute(defaultTest2Route);

    final Route defaultTest3Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST3).build();
    this.getRoutingTable().addRoute(defaultTest3Route);
  }

  /**
   * Accessor for the underlying {@link RoutingTable} used by this payment router.
   */
  RoutingTable<Route> getRoutingTable();
}
