package com.sappenin.ilpv4.connector.routing;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.model.RoutingTableId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * An implementation of {@link RoutingTable} that stores all routingTableEntries in-memory using an {@link
 * InterledgerAddressPrefixMap} for efficient search and prefix-matching operations.
 *
 * This implementation is meant for use-cases where routingTableEntries do not change very often, like
 * statically-configured routing environments where this table can be populated when the server starts-up.
 */
public class InMemoryRoutingTable implements RoutingTable<Route> {

  private final AtomicReference<RoutingTableId> routingTableId;
  private final AtomicLong currentEpoch;
  private final InterledgerAddressPrefixMap<Route> interledgerAddressPrefixMap;
  // Typed as an ArrayList for index-based lookups...
  private final List<RouteUpdate> routeUpdateLog;

  public InMemoryRoutingTable() {
    this.routingTableId = new AtomicReference<>(RoutingTableId.of(UUID.randomUUID()));
    this.currentEpoch = new AtomicLong();
    this.routeUpdateLog = Lists.newArrayList();
    this.interledgerAddressPrefixMap = new InterledgerAddressPrefixMap();
  }

  /**
   * Exists for testing purposes, but is otherwise not necessary.
   *
   * @param interledgerAddressPrefixMap
   */
  public InMemoryRoutingTable(final InterledgerAddressPrefixMap interledgerAddressPrefixMap) {
    this.routingTableId = new AtomicReference<>(RoutingTableId.of(UUID.randomUUID()));
    this.currentEpoch = new AtomicLong();
    this.routeUpdateLog = Lists.newArrayList();
    this.interledgerAddressPrefixMap = Objects.requireNonNull(interledgerAddressPrefixMap);
  }

  @Override
  public RoutingTableId getRoutingTableId() {
    return this.routingTableId.get();
  }

  public boolean compareAndSetRoutingTableId(
    final RoutingTableId expectRoutingTableId, final RoutingTableId newRoutingTableId
  ) {
    return this.routingTableId.compareAndSet(expectRoutingTableId, newRoutingTableId);
  }

  @Override
  public long getCurrentEpoch() {
    return this.currentEpoch.get();
  }

  public boolean compareAndSetCurrentEpoch(final long expectedEpoch, final long newEpoch) {
    return this.currentEpoch.compareAndSet(expectedEpoch, newEpoch);
  }

  // TODO: Fix this interface!
  @Override
  public boolean addRoute(
    final InterledgerAddressPrefix interledgerAddressPrefix, final Route route
  ) {
    Objects.requireNonNull(route);
    return this.interledgerAddressPrefixMap.add(route);
  }

  @Override
  public Collection<Route> removeRoute(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.removeAll(addressPrefix);
  }

  @Override
  public Collection<Route> getRoutesByPrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.getEntries(addressPrefix);
  }

  @Override
  public Collection<Route> removeAllRoutesForPrefix(InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.removeAll(addressPrefix);
  }

  @Override
  public void forEach(final BiConsumer<? super InterledgerAddressPrefix, ? super Collection<Route>> action) {
    Objects.requireNonNull(action);
    this.interledgerAddressPrefixMap.forEach(action);
  }

  @Override
  public Collection<Route> findNextHopRoutes(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress, "finalDestinationAddress must not be null!");
    return this.interledgerAddressPrefixMap.findNextHops(finalDestinationAddress);
  }

  /**
   * Reset the routing table to an empty state.
   */
  @Override
  public void reset() {

    // Don't allow anything else to occur in the prefix map while things are being reset...
    synchronized (interledgerAddressPrefixMap) {
      this.interledgerAddressPrefixMap.getPrefixMapKeys().forEach(interledgerAddressPrefixMap::removeAll);
    }
  }

  //  @Override
  //  public Collection<Route> findNextHopRoutes(
  //    final InterledgerAddress finalDestinationAddress,
  //    final InterledgerAddressPrefix sourcePrefix
  //  ) {
  //    Objects.requireNonNull(finalDestinationAddress);
  //    Objects.requireNonNull(sourcePrefix);
  //    return this.interledgerAddressPrefixMap.findNextHops(finalDestinationAddress).stream()
  //      // Only return routingTableEntries that are allowed per the source prefix filter...
  //      .filter(
  //        routingTableEntry -> routingTableEntry.getSourcePrefixRestrictionRegex()
  //          .matcher(sourcePrefix.getValue())
  //          .matches()
  //      )
  //      .collect(Collectors.toList());
  //  }


  @Override
  public List<RouteUpdate> getRouteUpdateLog() {
    return routeUpdateLog;
  }
}
