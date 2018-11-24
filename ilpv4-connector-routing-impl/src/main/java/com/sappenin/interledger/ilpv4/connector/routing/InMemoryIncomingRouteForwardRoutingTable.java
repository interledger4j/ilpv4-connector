package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An implementation of {@link RoutingTable} that stores all routingTableEntries in-memory using an {@link
 * InterledgerAddressPrefixMap} for efficient search and prefix-matching operations.
 *
 * This implementation is meant for use-cases where routingTableEntries do not change very often, like
 * statically-configured routing environments where this table can be populated when the server starts-up.
 */
public class InMemoryIncomingRouteForwardRoutingTable implements ForwardingRoutingTable<IncomingRoute> {

  private final AtomicReference<RoutingTableId> routingTableId;
  private final AtomicInteger currentEpoch;
  // Typed as an ArrayList for index-based lookups...
  private final ArrayList<IncomingRoute> incomingRouteLog;

  public InMemoryIncomingRouteForwardRoutingTable() {
    this.routingTableId = new AtomicReference<>(RoutingTableId.of(UUID.randomUUID()));
    this.currentEpoch = new AtomicInteger();
    this.incomingRouteLog = Lists.newArrayList();
  }

  @Override
  public RoutingTableId getRoutingTableId() {
    return this.routingTableId.get();
  }

  public boolean compareAndSetRoutingTableId(final RoutingTableId expectRoutingTableId, final RoutingTableId newRoutingTableId) {
    return this.routingTableId.compareAndSet(expectRoutingTableId, newRoutingTableId);
  }

  @Override
  public int getCurrentEpoch() {
    return this.currentEpoch.get();
  }

  public boolean compareAndSetCurrentEpoch(final int expectedEpoch, final int newEpoch) {
    return this.currentEpoch.compareAndSet(expectedEpoch, newEpoch);
  }

  @Override
  public boolean addRoute(InterledgerAddressPrefix addressPrefix, IncomingRoute incomingRoute) {
    return this.incomingRouteLog.add(incomingRoute);
  }

  @Override
  public Optional<IncomingRoute> getRouteForPrefix(InterledgerAddressPrefix addressPrefix) {
    return this.incomingRouteLog.stream()
      .filter(incomingRoute -> incomingRoute.getRoutePrefix().equals(addressPrefix))
      .findFirst();
  }

  @Override
  public boolean removeRoute(InterledgerAddressPrefix routePrefix) {
    return this.incomingRouteLog.removeIf((incomingRoute -> incomingRoute.getRoutePrefix().equals(routePrefix)));
  }

  @Override
  public void forEach(final Consumer<IncomingRoute> action) {
    Objects.requireNonNull(action);
    this.incomingRouteLog.forEach(action);
  }

  @Override
  public Iterable<IncomingRoute> getAllRoutes() {
    return this.incomingRouteLog;
  }

  /**
   * Get all prefixes that start with {@code interledgerAddressPrefix}.
   *
   * @param addressPrefix
   *
   * @return
   */
  @Override
  public List<InterledgerAddressPrefix> getKeysStartingWith(InterledgerAddressPrefix addressPrefix) {
    // TODO: This could be done *much* more efficiently, possibly using PatriciaTrie.
    return this.incomingRouteLog.stream()
      .filter(incomingRoute -> incomingRoute.getRoutePrefix().startsWith(addressPrefix))
      .map(IncomingRoute::getRoutePrefix)
      .collect(Collectors.toList());
  }

  @Override
  public Iterable<IncomingRoute> getPartialRouteLog(final int numberToSkip, final int limit) {
    return Iterables.limit(Iterables.skip(this.incomingRouteLog, numberToSkip), limit);
  }

  @Override
  public void resetEpochValue(int epoch) {
    this.incomingRouteLog.set(epoch, null);
  }

  @Override
  public void setEpochValue(final int epoch, final IncomingRoute route) {
    Objects.requireNonNull(route);
    this.incomingRouteLog.set(epoch, route);
  }
}
