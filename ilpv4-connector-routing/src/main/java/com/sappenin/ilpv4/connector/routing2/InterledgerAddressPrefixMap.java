package com.sappenin.ilpv4.connector.routing2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.interledger.core.InterledgerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * <p>A key/value Map keyed by {@link InterledgerAddress} to allow values of type {@link V} to be mapped by address
 * prefix, as opposed to exact-match semantics. For example, given an address of <tt>g.foo.bar</tt> both <tt>g .foo</tt>
 * and <tt>g</tt> are prefix-matches, and this data structure enables longest-prefix match operations.</p>
 *
 * <p>This data-structure can hold arbitrary values of type {@link V}, for example:
 * <pre>
 *   map.insert("foo", 1)
 *   map.insert("bar", 2)
 *   map.get("foo")     // ⇒ 1
 *   map.get("foo.bar") // ⇒ 1 ("foo" is the longest known prefix of "foo.bar")
 *   map.get("bar")     // ⇒ 2
 *   map.get("bar.foo") // ⇒ 2 ("bar" is the longest known prefix of "bar.foo")
 *   map.get("random")  // ⇒ Optional.empty
 * </pre>
 *
 * @param <V> The value type to place into this Prefix Map.
 */
public class InterledgerAddressPrefixMap<V> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PatriciaTrie<Collection<V>> prefixMap;

  public InterledgerAddressPrefixMap() {
    this.prefixMap = new PatriciaTrie<>();
  }

  /**
   * The current number of address-prefix keys in the map. This is distinct from the total number of values in the
   * prefix map.
   */
  public int getNumKeys() {
    return prefixMap.size();
  }

  /**
   * @param key
   * @param value
   *
   * @return
   */
  public boolean add(final InterledgerAddress key, final V value) {
    Objects.requireNonNull(value);

    final Collection<V> prefixedRouteSet;
    // Only allow a single thread to add a new value into this map at a time because the PatriciaTrie is not
    // thread-safe during puts.
    synchronized (prefixMap) {
      prefixedRouteSet = Optional.ofNullable(this.prefixMap.get(key.getValue()))
        .orElseGet(() -> {
          final Set<V> newPrefixedRoutes = Sets.newConcurrentHashSet();
          // Synchronized so that another thread doesn't add an identical value prefix from underneath us.
          this.prefixMap.put(key.getValue(), newPrefixedRoutes);
          return newPrefixedRoutes;
        });
    }

    // Add the value into the Set.
    // This is ok to perform outside of the critical section because the prefixedRouteSet Set is thread-safe.
    return prefixedRouteSet.add(value);
  }

  /**
   * Remove a single value from the Prefix Map.
   *
   * @param value A {@link V} to remove from the {@code prefixMap}.
   *
   * @return <tt>true</tt> if an element was removed as a result of this call.
   */
  public boolean remove(final InterledgerAddress key, final V value) {
    Objects.requireNonNull(value);

    synchronized (prefixMap) {
      // There will be only a single value in the routing table that can be removed. Find that, then remove it.
      final Collection<V> routeCollection = this.getValues(key);
      final boolean result = routeCollection.remove(value);
      if (result && routeCollection.isEmpty()) {
        // If there are no more routes in the table, then remove the entire collection so the getKeys works
        // properly.
        this.prefixMap.remove(key.getValue());
      }
      return result;
    }
  }

  /**
   * Remove all routes for the supplied {@code addressPrefix} key.
   */
  public Collection<V> removeAllValues(final InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    synchronized (prefixMap) {
      return this.prefixMap.remove(addressPrefix.getValue());
    }
  }

  public Collection<V> getValues(final InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    return Optional.ofNullable(this.prefixMap.get(addressPrefix.getValue())).orElse(Sets.newConcurrentHashSet());
  }

  /**
   * Returns a {@link Set} of keys that are contained in this map in {@link InterledgerAddress} form. Due to the
   * implementation of the PatriciaTrie (it stores Strings instead of ILP Address keys), the returned set is NOT backed
   * by the map, so changes to the map are not reflected in the set.
   *
   * @return a set view of the keys contained in this map
   */
  //  public Set<InterledgerAddress> getPrefixMapKeys() {
  //    return this.prefixMap.keySet().stream().map(InterledgerAddress::of).collect(Collectors.toSet());
  //  }

  /**
   * Take an action for each {@link V} in the PrefixMap.
   */
  public void forEach(final BiConsumer<? super InterledgerAddress, ? super Collection<V>> action) {
    Objects.requireNonNull(action);

    // Convert the BiConsumer input to the correct type.
    final BiConsumer<String, Collection<V>> stringBasedAccept = (k, v) -> action.accept(InterledgerAddress.of(k), v);

    this.prefixMap.forEach(stringBasedAccept);
  }

  // TODO: Move this method somewhere else....

  /**
   * Given an ILP final destination address, determine the longest-matching target address in the routing table, and
   * then return all routes that exist for that target address.
   *
   * @param finalDestinationAddress An ILP prefix address of type {@link InterledgerAddress}.
   */
  public Collection<V> findNextHopRoutes(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);

    return finalDestinationAddress.getParentAddress()
      .map(this::findLongestPrefix)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(longestPrefix -> this.prefixMap.get(longestPrefix))
      .orElse(ImmutableList.of());
  }


  /**
   * Helper method to find the longest-prefix match given a destination ILP address.
   *
   * Because the PatriciaTrie allows for all entries with a given prefix to be cheaply returned from the map, we first
   * search the Trie for entries that contain a substring of the destination address up to the last period (.)
   * character. If a match is found, then all routes for the match are returned. However, if no match is found, then a
   * substring of the destination address is used, starting at index 0, up to the last period (.) character (exclusive),
   * moving from the end of destinationAddress backwards towards index 0.
   *
   * After repeating this process back through the {@code destinationAddress}, if no matches in the prefix map are
   * found, then this method returns {@link Optional#empty()}.
   *
   * @param key An {@link InterledgerAddress} key to look into the map by.
   *
   * @return The longest-prefix match in the PatriciaTrie for the supplied {@code destinationAddress}.
   */
  @VisibleForTesting
  protected Optional<String> findLongestPrefix(final InterledgerAddress key) {
    Objects.requireNonNull(key);

    // Unlike a typical prefix match, ILP addresses can be matched by subsections delimited by a period separator.

    // 1.) Narrow the overall search-space by using the PatriciaTrie to filter out any non-matching prefixes
    // (e.g. for destAddress of "g.foo.bar.baz", the entries "g." and "g.foo." will not be returned by the
    // prefixMap. This allows us to find a longest-match in that space. If there aren't any matches, we
    // recurse this method (#findLongestPrefix) using the parent-prefix. For example, if the routing-table has
    // only "g", then "g.1.2" will not return a sub-map, so we should recursively search for a sub-map with
    // with "g.1", and then "g".

    final SortedMap<String, Collection<V>> prefixSubMap = prefixMap.prefixMap(key.getValue());
    if (prefixSubMap.isEmpty() && key.getParentAddress().isPresent()) {
      final InterledgerAddress parentPrefix = key.getParentAddress().get();
      final Optional<String> longestMatch = this.findLongestPrefix(parentPrefix);
      if (longestMatch.isPresent()) {
        return longestMatch;
      } else {
        // Fallback to a global-prefix match, if any is defined in the routing table.
        return this.findLongestPrefix(getRootPrefix(key));
      }
    } else {
      // There are prefixes in the Trie to search. So, we loop through each one in this reduced search space.
      // This is effectively an O(n) operation with n being the number of entries in prefixSubMap.
      return prefixSubMap.keySet().stream()
        .filter(val -> val.length() <= key.getValue().length())
        .distinct()
        // Don't allow more than one in this list.
        .collect(Collectors.reducing((a, b) -> {
          logger.error(
            "Routing table has more than one longest-match! This should never happen!"
          );
          return null;
        }));
    }
  }

  /**
   * Compute the root-prefix of the supplied {@code address}.
   *
   * @return An {@link InterledgerAddress} representing the root prefix for the supplied Interledger address.
   */
  @VisibleForTesting
  protected InterledgerAddress getRootPrefix(final InterledgerAddress address) {
    Objects.requireNonNull(address);

    while (address.getParentAddress().isPresent()) {
      return getRootPrefix(address.getParentAddress().get());
    }

    return address;
  }

}
