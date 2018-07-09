package com.sappenin.ilpv4.connector.routing2;

import org.interledger.core.InterledgerAddress;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * An implementation of {@link RoutingTable} that stores all routes in-memory using an {@link
 * InterledgerAddressPrefixMap} for efficient search and prefix-matching operations.
 *
 * This implementation is meant for use-cases where routes do not change very often, like statically-configured routing
 * environments where this table can be populated when the server starts-up.
 */
public class InMemoryRoutingTable implements RoutingTable<Route> {

  private final InterledgerAddressPrefixMap<Route> interledgerPrefixMap;

  public InMemoryRoutingTable() {
    this(new InterledgerAddressPrefixMap());
  }

  /**
   * Exists for testing purposes, but is otherwise not necessary.
   *
   * @param interledgerPrefixMap
   */
  public InMemoryRoutingTable(final InterledgerAddressPrefixMap interledgerPrefixMap) {
    this.interledgerPrefixMap = Objects.requireNonNull(interledgerPrefixMap);
  }

  @Override
  public boolean addRoute(final Route route) {
    Objects.requireNonNull(route);
    return this.interledgerPrefixMap.add(route.getNextHopAccount(), route);
  }

  @Override
  public boolean removeRoute(final Route route) {
    Objects.requireNonNull(route);
    return this.interledgerPrefixMap.remove(route.getNextHopAccount(), route);
  }

  @Override
  public Collection<Route> getRoutesByTargetPrefix(final InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerPrefixMap.getValues(addressPrefix);
  }

  @Override
  public Collection<Route> removeAllRoutesForTargetPrefix(InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerPrefixMap.removeAllValues(addressPrefix);
  }

  @Override
  public void forEach(final BiConsumer<? super InterledgerAddress, ? super Collection<Route>> action) {
    Objects.requireNonNull(action);
    this.interledgerPrefixMap.forEach(action);
  }

  @Override
  public Collection<Route> findNextHopRoutes(InterledgerAddress finalDestinationAddress) {
    return this.interledgerPrefixMap.findNextHopRoutes(finalDestinationAddress);
  }

  //  @Override
  //  public Collection<Route> findNextHopRoutes(
  //    final InterledgerAddress finalDestinationAddress,
  //    final InterledgerAddress sourcePrefix
  //  ) {
  //    Objects.requireNonNull(finalDestinationAddress);
  //    Objects.requireNonNull(sourcePrefix);
  //    return this.interledgerPrefixMap.findNextHopRoutes(finalDestinationAddress).stream()
  //      // Only return routes that are allowed per the source prefix filter...
  //      .filter(route -> route.getSourcePrefixRestrictionRegex().matcher(sourcePrefix.getValue()).matches())
  //      .collect(Collectors.toList());
  //  }
}
