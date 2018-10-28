package com.sappenin.ilpv4.connector.routing;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.connector.ccp.CcpSyncMode;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A simple implementation of {@link PaymentRouter} that chooses the first getRoute if multiple are returned from the
 * routing table.
 */
public class SimplePaymentRouter implements PaymentRouter<Route> {
  private final RoutingTable<Route> routingTable;

  // Begins life in IDLE mode...
  private final Map<InterledgerAddress, CcpSyncMode> syncModes;

  public SimplePaymentRouter(final RoutingTable<Route> routingTable) {
    this.syncModes = Maps.newConcurrentMap();
    this.routingTable = Objects.requireNonNull(routingTable);
  }

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" getRoute that
   * should be utilized to complete an Interledger payment.
   *
   * At a general level, this method works as follows:
   *
   * Given an ILP Payment from A→C, find the next hop B on the payment pathParts from A to C.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   */
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    return this.routingTable.findNextHopRoute(finalDestinationAddress);
  }

  /**
   * Accessor for the underlying {@link RoutingTable} used by this payment router.
   */
  @Override
  public RoutingTable<Route> getRoutingTable() {
    return this.routingTable;
  }

  /**
   * Accessor for the underlying {@link RoutingTable} used by this payment router.
   *
   * @param defaultDestinationAddress
   */
  @Override
  public void setDefaultRoute(InterledgerAddress defaultDestinationAddress) {

    // Add a default global route for this account...
    final Route defaultGlobalRoute = ImmutableRoute.builder()
      .routePrefix(InterledgerAddressPrefix.GLOBAL)
      .expiresAt(Instant.MAX)
      .nextHopAccount(defaultDestinationAddress)
      .build();
    this.routingTable.addRoute(InterledgerAddressPrefix.GLOBAL, defaultGlobalRoute);

    final Route defaultTestRoute =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST).build();
    this.routingTable.addRoute(InterledgerAddressPrefix.TEST, defaultTestRoute);

    final Route defaultTest1Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST1).build();
    this.routingTable.addRoute(InterledgerAddressPrefix.TEST1, defaultTest1Route);

    final Route defaultTest2Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST2).build();
    this.routingTable.addRoute(InterledgerAddressPrefix.TEST2, defaultTest2Route);

    final Route defaultTest3Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST3).build();
    this.routingTable.addRoute(InterledgerAddressPrefix.TEST3, defaultTest3Route);
  }

  //  /**
  //   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" getRoute that should
  //   * be utilized to complete an Interledger payment.
  //   *
  //   * At a general level, this method works as follows:
  //   *
  //   * Given an ILP Payment from A→C, find the next hop B on the payment pathParts from A to C.
  //   *
  //   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
  //   *                                payment or message (this address may or may not be locally accessible in the routing
  //   *                                table).
  //   * @param sourceLedgerPrefix      An {@link InterledgerAddress} prefix that indicates the ILP node that received the
  //   *                                payment being routed. This value is used to optionally restrict the set of
  //   *                                available
  //   */
  //  @Override
  //  public Optional<Route> findBestNexHop(InterledgerAddress finalDestinationAddress,
  //                                                    InterledgerAddressPrefix sourceLedgerPrefix) {
  //    return this.routingTable.findNextHopRoute(finalDestinationAddress, sourceLedgerPrefix).stream().findFirst();
  //  }

}
