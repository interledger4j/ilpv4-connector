package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.InterledgerAddressPrefix;
import org.interledger.core.InterledgerAddress;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * An implementation of {@link RoutingTable} that stores all routingTableEntrys in-memory using an {@link
 * InterledgerAddressPrefixMap} for efficient search and prefix-matching operations.
 *
 * This implementation is meant for use-cases where routingTableEntrys do not change very often, like
 * statically-configured routing environments where this table can be populated when the server starts-up.
 */
public class InMemoryRoutingTable implements RoutingTable<RoutingTableEntry> {

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
    this.interledgerAddressPrefixMap = Objects.requireNonNull(interledgerAddressPrefixMap);
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
      // Only return routingTableEntrys that are allowed per the source prefix filter...
      .filter(routingTableEntry -> routingTableEntry.getSourcePrefixRestrictionRegex().matcher(sourcePrefix.getValue())
        .matches())
      .collect(Collectors.toList());
  }
}
