package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * An implementation of {@link RoutingTable} that stores all routingTableEntries in-memory using an {@link
 * InterledgerAddressPrefixMap} for efficient search and prefix-matching operations.
 *
 * This implementation is meant for use-cases where routingTableEntries do not change very often, like
 * statically-configured routing environments where this table can be populated when the server starts-up.
 */
public class InMemoryRoutingTable implements RoutingTable<RoutingTableEntry> {

  private final UUID routingTableId;
  private final AtomicLong currentEpoch;
  private final InterledgerAddressPrefixMap<RoutingTableEntry> interledgerAddressPrefixMap;

  public InMemoryRoutingTable() {
    this(new InterledgerAddressPrefixMap());
  }

  /**
   * Exists for testing purposes, but is otherwise not necessary.
   *
   * @param interledgerAddressPrefixMap
   */
  public InMemoryRoutingTable(final InterledgerAddressPrefixMap interledgerAddressPrefixMap) {
    this.routingTableId = UUID.randomUUID();
    this.currentEpoch = new AtomicLong();
    this.interledgerAddressPrefixMap = Objects.requireNonNull(interledgerAddressPrefixMap);
  }

  @Override
  public UUID getRoutingTableId() {
    return this.routingTableId;
  }

  @Override
  public long getCurrentEpoch() {
    return this.currentEpoch.get();
  }

  @Override
  public boolean addRoute(final RoutingTableEntry routingTableEntry) {
    Objects.requireNonNull(routingTableEntry);
    return this.interledgerAddressPrefixMap.add(routingTableEntry);
  }

  @Override
  public boolean removeRoute(final RoutingTableEntry routingTableEntry) {
    Objects.requireNonNull(routingTableEntry);
    return this.interledgerAddressPrefixMap.remove(routingTableEntry);
  }

  @Override
  public Collection<RoutingTableEntry> getRoutesByTargetPrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.getEntries(addressPrefix);
  }

  @Override
  public Collection<RoutingTableEntry> removeAllRoutesForTargetPrefix(InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.removeAll(addressPrefix);
  }

  @Override
  public void forEach(final BiConsumer<? super String, ? super Collection<RoutingTableEntry>> action) {
    Objects.requireNonNull(action);
    this.interledgerAddressPrefixMap.forEach(action);
  }

  @Override
  public Collection<RoutingTableEntry> findNextHopRoutes(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress, "finalDestinationAddress must not be null!");
    return this.interledgerAddressPrefixMap.findNextHops(finalDestinationAddress);
  }

  @Override
  public Collection<RoutingTableEntry> findNextHopRoutes(
    final InterledgerAddress finalDestinationAddress,
    final InterledgerAddressPrefix sourcePrefix
  ) {
    Objects.requireNonNull(finalDestinationAddress);
    Objects.requireNonNull(sourcePrefix);
    return this.interledgerAddressPrefixMap.findNextHops(finalDestinationAddress).stream()
      // Only return routingTableEntries that are allowed per the source prefix filter...
      .filter(
        routingTableEntry -> routingTableEntry.getSourcePrefixRestrictionRegex()
          .matcher(sourcePrefix.getValue())
          .matches()
      )
      .collect(Collectors.toList());
  }


  @Override
  public ArrayList<RouteUpdate> getRouteUpdateLog() {
    throw new RuntimeException("Not yet implemented!");
  }
}
