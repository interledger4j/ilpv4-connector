package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * An implementation of {@link RoutingTable} that stores all routes in-memory using an {@link
 * InterledgerAddressPrefixMap} for efficient search and prefix-matching operations.
 *
 * This implementation is meant for use-cases where routes do not change very often, like statically-configured routing
 * environments where this table can be populated when the server starts-up.
 */
public class InMemoryRoutingTable<R extends BaseRoute> implements RoutingTable<R> {

  private final InterledgerAddressPrefixMap<R> interledgerAddressPrefixMap;

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

  //  @Override
  //  public RoutingTableId getRoutingTableId() {
  //    return this.routingTableId.get();
  //  }

  //  public boolean compareAndSetRoutingTableId(
  //    final RoutingTableId expectRoutingTableId, final RoutingTableId newRoutingTableId
  //  ) {
  //    return this.routingTableId.compareAndSet(expectRoutingTableId, newRoutingTableId);
  //  }
  //
  //  public boolean compareAndSetCurrentEpoch(final long expectedEpoch, final long newEpoch) {
  //    return this.currentEpoch.compareAndSet(expectedEpoch, newEpoch);
  //  }

  @Override
  public R addRoute(final R route) {
    Objects.requireNonNull(route);
    return this.interledgerAddressPrefixMap.putEntry(route.routePrefix(), route);
  }

  @Override
  public Optional<R> removeRoute(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.removeEntry(addressPrefix);
  }

  @Override
  public Optional<R> getRouteByPrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return this.interledgerAddressPrefixMap.getEntry(addressPrefix);
  }

  // TODO: Is this possible with the trie? E.g., if the trie has g.foo and g.foo.bar, can we removeEntry "g"?
  //  @Override
  //  public Collection<Route> removeAllRoutesForPrefix(InterledgerAddressPrefix addressPrefix) {
  //    Objects.requireNonNull(addressPrefix);
  //    return this.interledgerAddressPrefixMap.removeAll(addressPrefix);
  //  }

  @Override
  public Iterable<InterledgerAddressPrefix> getAllPrefixes() {
    return this.interledgerAddressPrefixMap.getKeys();
  }

  @Override
  public void forEach(final BiConsumer<InterledgerAddressPrefix, R> action) {
    Objects.requireNonNull(action);
    this.interledgerAddressPrefixMap.forEach(action);
  }

  @Override
  public Optional<R> findNextHopRoute(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "finalDestinationAddress must not be null!");
    return this.interledgerAddressPrefixMap.findNextHop(interledgerAddress);
  }

  @Override
  public void reset() {
    this.interledgerAddressPrefixMap.reset();
  }
}
