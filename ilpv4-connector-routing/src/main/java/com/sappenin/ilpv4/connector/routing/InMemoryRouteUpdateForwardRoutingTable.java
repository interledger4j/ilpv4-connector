package com.sappenin.ilpv4.connector.routing;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sappenin.ilpv4.model.RoutingTableId;
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
public class InMemoryRouteUpdateForwardRoutingTable implements ForwardingRoutingTable<RouteUpdate> {

  private final AtomicReference<RoutingTableId> routingTableId;
  private final AtomicInteger currentEpoch;
  // Typed as an ArrayList for index-based lookups...
  private final ArrayList<RouteUpdate> routeUpdateLog;

  public InMemoryRouteUpdateForwardRoutingTable() {
    this.routingTableId = new AtomicReference<>(RoutingTableId.of(UUID.randomUUID()));
    this.currentEpoch = new AtomicInteger();
    this.routeUpdateLog = Lists.newArrayList();
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
  public boolean addRoute(InterledgerAddressPrefix addressPrefix, RouteUpdate routeUpdate) {
    return this.routeUpdateLog.add(routeUpdate);
  }

  @Override
  public Optional<RouteUpdate> getRouteForPrefix(InterledgerAddressPrefix addressPrefix) {
    return this.routeUpdateLog.stream()
      .filter(routeUpdate -> routeUpdate.getRoutePrefix().equals(addressPrefix))
      .findFirst();
  }

  @Override
  public boolean removeRoute(InterledgerAddressPrefix routePrefix) {
    return this.routeUpdateLog.removeIf((routeUpdate -> routeUpdate.getRoutePrefix().equals(routePrefix)));
  }

  @Override
  public void forEach(final Consumer<RouteUpdate> action) {
    Objects.requireNonNull(action);
    this.routeUpdateLog.forEach(action);
  }

  @Override
  public Iterable<RouteUpdate> getAllRoutes() {
    return this.routeUpdateLog;
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
    return this.routeUpdateLog.stream()
      .filter(routeUpdate -> routeUpdate.getRoutePrefix().startsWith(addressPrefix))
      .map(RouteUpdate::getRoutePrefix)
      .collect(Collectors.toList());
  }

  @Override
  public Iterable<RouteUpdate> getPartialRouteLog(final int numberToSkip, final int limit) {
    return Iterables.limit(Iterables.skip(this.routeUpdateLog, numberToSkip), limit);
  }

  @Override
  public void resetEpochValue(int epoch) {
    this.routeUpdateLog.set(epoch, null);
  }

  @Override
  public void setEpochValue(final int epoch, final RouteUpdate routeUpdate) {
    Objects.requireNonNull(routeUpdate);
    this.routeUpdateLog.set(epoch, routeUpdate);
  }
}
