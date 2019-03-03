package com.sappenin.interledger.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * <p>A Routing Table is a lookup table of routes, indexed by prefix. This type of routing table is used to forward
 * routes to remote peers.</p>
 */
public interface ForwardingRoutingTable<R extends BaseRoute> {

  /**
   * The unique identifier of this routing table, primarily used for coordinating Route updates via CCP.
   *
   * @return A {@link UUID}.
   */
  RoutingTableId getRoutingTableId();

  /**
   * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
   *
   * @param expectedRoutingTableId the expected value
   * @param newRoutingTableId      the new value
   *
   * @return {@code true} if successful. False return indicates that the actual value was not equal to the expected
   * value.
   */
  boolean compareAndSetRoutingTableId(RoutingTableId expectedRoutingTableId, RoutingTableId newRoutingTableId);

  /**
   * <p>Accessor for the current getEpoch of this routing table.</p>
   *
   * <p>Every time the routing table is modified, the update is logged and the revision number of the table is
   * increased. The revision number of the table is called an **getEpoch**.</p>
   *
   * @return A <tt>int</tt>.
   */
  default int getCurrentEpoch() {
    return 0;
  }

  /**
   * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
   *
   * @param expectedEpoch the expected value
   * @param newEpoch      the new value
   *
   * @return {@code true} if successful. False return indicates that the actual value was not equal to the expected
   * value.
   */
  boolean compareAndSetCurrentEpoch(int expectedEpoch, int newEpoch);

  /**
   * Add a getRoute to this routing table. If the getRoute already exists (keyed by {@code prefix}, then this operation
   * is a no-op.
   *
   * @param route A {@link BaseRoute} to add to this routing table.
   *
   * @return {@code true} if any elements were added
   */
  boolean addRoute(InterledgerAddressPrefix addressPrefix, R route);

  /**
   * Retrieve the getRoute corresponding to {@code addressPrefix}.
   *
   * @param addressPrefix
   *
   * @return
   */
  Optional<R> getRouteForPrefix(InterledgerAddressPrefix addressPrefix);

  /**
   * Removes the getRoute identified by {@code routePrefix}.
   *
   * @param routePrefix The unique key of the getRoute to removeEntry.
   *
   * @return {@code true} if any elements were removed
   */
  boolean removeRoute(InterledgerAddressPrefix routePrefix);

  /**
   * Perform the following action on each item in the routing table.
   *
   * @param action A {@link Consumer} to apply to each element in the getRoute-update log.
   */
  void forEach(final Consumer<R> action);

  /**
   * Returns a view of Route Updates.
   *
   * @return An {@link Iterable} view of the underlying getRoute update log.
   */
  Iterable<R> getAllRoutes();

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
