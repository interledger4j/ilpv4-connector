package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An implementation of {@link RoutingTable} that stores all {@link RouteUpdate} entries in-memory using an {@link
 * InterledgerAddressPrefixMap} for efficient search and prefix-matching operations.
 */
public class InMemoryForwardingRoutingTable extends InMemoryRoutingTable<RouteUpdate> implements
  ForwardingRoutingTable<RouteUpdate> {

  private final AtomicReference<RoutingTableId> routingTableId;
  private final AtomicInteger currentEpoch;
  // Typed as an SortedMap as opposed to an Array to both allow for index-based lookups (like an Array) as well as
  // allow for insertions into array-like indexes that may not exist.
  private final SortedMap<Integer, RouteUpdate> routeUpdateLog;

  public InMemoryForwardingRoutingTable() {
    this.routingTableId = new AtomicReference<>(RoutingTableId.of(UUID.randomUUID()));
    this.currentEpoch = new AtomicInteger();
    this.routeUpdateLog = Maps.newTreeMap();
  }

  @Override
  public RoutingTableId getRoutingTableId() {
    return this.routingTableId.get();
  }

  @Override
  public int getCurrentEpoch() {
    return this.currentEpoch.get();
  }

  @Override
  public List<InterledgerAddressPrefix> getKeysStartingWith(InterledgerAddressPrefix addressPrefix) {
    // TODO: This could be done *much* more efficiently
    return StreamSupport.stream(this.getAllPrefixes().spliterator(), false)
      .filter(prefix -> prefix.startsWith(addressPrefix))
      .collect(Collectors.toList());
  }

  @Override
  public Iterable<RouteUpdate> getPartialRouteLog(final int numberToSkip, final int limit) {
    return Iterables.limit(Iterables.skip(this.routeUpdateLog.values(), numberToSkip), limit);
  }

  @Override
  public void resetEpochValue(final int epoch) {
    this.routeUpdateLog.put(epoch, null);
  }

  @Override
  public void setEpochValue(final int epoch, final RouteUpdate routeUpdate) {
    Objects.requireNonNull(routeUpdate);
    this.routeUpdateLog.put(epoch, routeUpdate);
  }
}
