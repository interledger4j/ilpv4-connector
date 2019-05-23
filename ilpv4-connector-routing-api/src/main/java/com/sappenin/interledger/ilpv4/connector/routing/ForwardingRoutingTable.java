package com.sappenin.interledger.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

import java.util.List;
import java.util.UUID;

/**
 * <p>A Routing Table is a lookup table of routes, indexed by prefix. This type of routing table is used to track
 * routes that will be forwarded to remote peers by tracking instances of {@link RouteUpdate} in a prefix-matched
 * instance of {@link RoutingTable} as well as tracking a log of updates that should be forwarded to remote peers.</p>
 *
 * @see "https://github.com/interledger/rfcs/blob/55da909af39a81fb53d8e64d3411c14716b98259/0000-route-broadcasting-protocol/0000-route-broadcasting-protocol.md#types-of-routing-table"
 */
public interface ForwardingRoutingTable<R extends RouteUpdate> extends RoutingTable<R> {

  /**
   * The unique identifier of this routing table, primarily used for coordinating Route updates via CCP.
   *
   * @return A {@link UUID}.
   */
  RoutingTableId getRoutingTableId();

  /**
   * <p>Accessor for the current epoch of this routing table.</p>
   *
   * <p>Every time the routing table is modified, the update is logged and the revision number of the table is
   * increased. The revision number of the table is called an **epoch**.</p>
   *
   * @return A <tt>int</tt>.
   */
  default int getCurrentEpoch() {
    return 0;
  }

  /**
   * Get all prefixes that start with {@code interledgerAddressPrefix}.
   *
   * @param interledgerAddressPrefix
   *
   * @return
   */
  List<InterledgerAddressPrefix> getKeysStartingWith(InterledgerAddressPrefix interledgerAddressPrefix);

  /**
   * Returns a view of {@code iterable} that skips its first {@code numberToSkip} elements and is limited to {@code
   * limit} elements. If {@code iterable} contains fewer than {@code numberToSkip} elements, the returned iterable skips
   * all of its elements.
   *
   * @param numberToSkip The number of elements to skip.
   * @param limit        The maximum number of elements to return.
   *
   * @return An {@link Iterable} view of the underlying getRoute update log.
   */
  Iterable<R> getPartialRouteLog(final int numberToSkip, final int limit);

  void resetEpochValue(int epoch);

  void setEpochValue(int epoch, R route);
}
