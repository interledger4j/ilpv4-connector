package com.sappenin.interledger.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Objects;
import java.util.Optional;
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

  public InMemoryRoutingTable() {
    this.routingTableId = new AtomicReference<>(RoutingTableId.of(UUID.randomUUID()));
    this.currentEpoch = new AtomicLong();
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

//  @Override
//  public Route addRoute(
//    final InterledgerAddressPrefix interledgerAddressPrefix, final Route route
//  ) {
//    Objects.requireNonNull(route);
//    return this.interledgerAddressPrefixMap.putEntry(interledgerAddressPrefix, route);
//  }

  @Override
  public Route addRoute(final Route route) {
    Objects.requireNonNull(route);
    return this.interledgerAddressPrefixMap.putEntry(route.getRoutePrefix(), route);
  }

  @Override
  public Optional<Route> removeRoute(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.removeEntry(addressPrefix);
  }

  @Override
  public Optional<Route> getRouteByPrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.getEntry(addressPrefix);
  }

  // TODO: Is this possible with the trie? E.g., if the trie has g.foo and g.foo.bar, can we removeEntry "g"?
  //  @Override
  //  public Collection<Route> removeAllRoutesForPrefix(InterledgerAddressPrefix addressPrefix) {
  //    Objects.requireNonNull(addressPrefix);
  //    return this.interledgerAddressPrefixMap.removeAll(addressPrefix);
  //  }

  /**
   * Obtain a view of all Routes in this Routing Table.
   *
   * @return
   */
  @Override
  public Iterable<InterledgerAddressPrefix> getAllPrefixes() {
    return this.interledgerAddressPrefixMap.getKeys();
  }

  @Override
  public void forEach(final BiConsumer<InterledgerAddressPrefix, Route> action) {
    Objects.requireNonNull(action);
    this.interledgerAddressPrefixMap.forEach(action);
  }

  @Override
  public Optional<Route> findNextHopRoute(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress, "finalDestinationAddress must not be null!");
    return this.interledgerAddressPrefixMap.findNextHop(finalDestinationAddress);
  }

  /**
   * Reset the routing table to an empty state.
   */
  @Override
  public void reset() {
    this.interledgerAddressPrefixMap.reset();
  }
}
