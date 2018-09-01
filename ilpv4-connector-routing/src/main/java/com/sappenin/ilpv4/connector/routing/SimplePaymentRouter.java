package com.sappenin.ilpv4.connector.routing;

import com.google.common.collect.Maps;
import org.interledger.core.InterledgerAddressPrefix;
import com.sappenin.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.ilpv4.connector.ccp.CcpSyncMode;
import org.interledger.core.InterledgerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A simple implementation of {@link PaymentRouter} that simply chooses the first route if multiple are returned from
 * the routing table.
 */
public class SimplePaymentRouter implements PaymentRouter<RoutingTableEntry> {
  private final RoutingTable<RoutingTableEntry> routingTable;

  // Begins life in IDLE mode...
  private final Map<InterledgerAddress, CcpSyncMode> syncModes;

  public SimplePaymentRouter(final RoutingTable<RoutingTableEntry> routingTable) {
    this.syncModes = Maps.newConcurrentMap();
    this.routingTable = Objects.requireNonNull(routingTable);
  }

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" route that should
   * be utilized to complete an Interledger payment.
   *
   * At a general level, this method works as follows:
   *
   * Given an ILP Payment from A→C, find the next hop B on the payment pathParts from A to C.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   */
  public Optional<RoutingTableEntry> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    return this.routingTable.findNextHopRoutes(finalDestinationAddress).stream().findFirst();
  }

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" route that should
   * be utilized to complete an Interledger payment.
   *
   * At a general level, this method works as follows:
   *
   * Given an ILP Payment from A→C, find the next hop B on the payment pathParts from A to C.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   * @param sourceLedgerPrefix      An {@link InterledgerAddress} prefix that indicates the ILP node that received the
   *                                payment being routed. This value is used to optionally restrict the set of
   *                                available
   */
  @Override
  public Optional<RoutingTableEntry> findBestNexHop(InterledgerAddress finalDestinationAddress,
                                                    InterledgerAddressPrefix sourceLedgerPrefix) {
    return this.routingTable.findNextHopRoutes(finalDestinationAddress, sourceLedgerPrefix).stream().findFirst();
  }


}
