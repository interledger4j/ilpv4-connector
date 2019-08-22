package org.interledger.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.interledger.connector.routing.BaseRoute;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A key/value data structure that holds {@link InterledgerAddressPrefix} keys in a hierarchical order to allow for easy
 * prefix-matching, supporting multiple prefixes per key, similar to a multi-map.
 */
public class InterledgerAddressPrefixMultiMap<R extends BaseRoute> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PatriciaTrie<Collection<R>> prefixMap;

  public InterledgerAddressPrefixMultiMap() {
    this.prefixMap = new PatriciaTrie<>();
  }

  /**
   * The current number of address-prefix keys in the map. This is distinct from the total number of routes in the Map.
   */
  public int getNumKeys() {
    return prefixMap.size();
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException {@inheritDoc}
   */
  public boolean add(final R route) {
    Objects.requireNonNull(route);

    final Collection<R> prefixedRouteSet;
    // Only allow a single thread to add a new getRoute into this map at a time because the PatriciaTrie is not
    // thread-safe during puts.
    synchronized (prefixMap) {
      prefixedRouteSet = Optional.ofNullable(
        this.prefixMap.get(toTrieKey(route.getRoutePrefix())))
        .orElseGet(() -> {
          final Set<R> newPrefixedRoutes = Sets.newConcurrentHashSet();
          // Synchronized so that another thread doesn't add an identical getRoute prefix from underneath us.
          this.prefixMap.put(toTrieKey(route.getRoutePrefix()), newPrefixedRoutes);
          return newPrefixedRoutes;
        });
    }

    // This is ok to perform outside of the critical section because the prefixedRouteSet Set is thread-safe
    // (though this might not hurt to do inside of the synchronized block)
    return prefixedRouteSet.add(route);
  }

  /**
   * Remove all routes for the supplied {@code addressPrefix} key.
   */
  public Collection<R> removeAll(final InterledgerAddressPrefix targetAddressPrefix) {
    Objects.requireNonNull(targetAddressPrefix);
    synchronized (prefixMap) {
      return this.prefixMap.remove(toTrieKey(targetAddressPrefix));
    }
  }

  public Collection<R> getEntries(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix, "addressPrefix must not be null!");

    return Optional.ofNullable(this.prefixMap.get(toTrieKey(addressPrefix)))
      .orElse(Sets.newConcurrentHashSet());
  }

  /**
   * Returns a {@link Set} of keys that are contained in this map in {@link InterledgerAddress} form. Due to the
   * implementation of the PatriciaTrie (it stores Strings instead of ILP Address keys), the returned set is NOT backed
   * by the map, so changes to the map are not reflected in the set.
   *
   * @return a set view of the keys contained in this map
   */
  public Set<InterledgerAddressPrefix> getPrefixMapKeys() {
    return this.prefixMap.keySet().stream()
      .map(this::stripTrailingDot)
      .map(InterledgerAddressPrefix::of)
      .collect(Collectors.toSet());
  }

  /**
   * Take an action for each {@link R} in the PrefixMap.
   *
   * @param action An instance of {@link BiConsumer} that provides two inputs, the ILP address of a remote peer, and a
   *               Collection of routing table entries of type {@link BaseRoute}.
   */
  public void forEach(final BiConsumer<? super InterledgerAddressPrefix, ? super Collection<R>> action) {
    Objects.requireNonNull(action);

    // In order to utilize this method with the prefixMap, we need to convert the BiConsumer into a String input from
    // an ILP address.
    final BiConsumer<String, Collection<R>> translatedAction = (ilpPrefixAsString, routingTableEntries) -> action
      .accept(InterledgerAddressPrefix.of(ilpPrefixAsString), routingTableEntries);

    // This call will take each value from prefixMap (which stores as a String) and then translate the values to
    // typed equivalents, and then perform the supplied `action`.
    prefixMap.forEach(translatedAction);
  }

  /**
   * Helper method to find the longest-prefix match given a destination ILP address (as a <tt>String</tt>).
   *
   * Because the PatriciaTrie allows for all entries with a given prefix to be cheaply returned from the map, we first
   * search the Trie for entries that contain a substring of the destination address up to the last period (.)
   * character. If a match is found, then all routes for the match are returned. However, if no match is found, then a
   * substring of the destination address is used, starting at index 0, up to the last period (.) character, moving from
   * the end of destinationAddress backwards towards index 0.
   *
   * After repeating this process back through the {@code destinationAddress}, if no matches in the routing table are
   * found, then this method returns {@link Optional#empty()}.
   *
   * @param destinationAddressPrefix A {@link String} representing a destination ILP address.
   *
   * @return The longest-prefix match in the PatriciaTrie for the supplied {@code destinationAddress}.
   */
  @VisibleForTesting
  protected Optional<InterledgerAddressPrefix> findLongestPrefix(
    final InterledgerAddressPrefix destinationAddressPrefix
  ) {
    Objects.requireNonNull(destinationAddressPrefix, "destinationAddressPrefix must not be null!");

    // Unlike a typical prefix match, ILP addresses can be matched by subsections delimited by a period separator.

    // 1.) Narrow the overall search-space by using the PatriciaTrie to filter out any non-matching prefixes
    // (e.g. for destAddress of "g.foo.bar.baz", the entries "g" and "g.foo" will not be returned by the
    // PatriciaTrie. This allows us to find a longest-match in the "g.foo.bar.baz" space. If there aren't any
    // matches, we recurse this method (#findLongestPrefix) using the parent-prefix. For example, if the
    // routing-table has only "g", then "g.1.2." will not return a sub-map, so we should recursively search for a
    // sub-map with with "g.1" (which would return nothing), and then "g".

    final SortedMap<String, Collection<R>> prefixSubMap = prefixMap.prefixMap(
      toTrieKey(destinationAddressPrefix)
    );
    if (prefixSubMap.isEmpty() && destinationAddressPrefix.hasPrefix()) {
      // The PatriciaTrie has no prefixes, so try this whole thing again with the parent prefix.
      return destinationAddressPrefix.getPrefix()
        .map(parentPrefix -> {
          final Optional<InterledgerAddressPrefix> longestMatch = this.findLongestPrefix(parentPrefix);
          if (longestMatch.isPresent()) {
            return longestMatch;
          } else {

            // Fallback to a global-prefix match, if any is defined in the routing table.
            return this.findLongestPrefix(destinationAddressPrefix.getRootPrefix());

            //            // Search for the parent prefix, if possible, or else return `empty`.
            //            return parentPrefix.getPrefix()
            //              .map(this::findLongestPrefix)
            //              .orElse(Optional.empty());
          }
        })
        // Should never occur due to destinationAddressPrefix.getPrefix().isPresent() being true above...
        .orElse(Optional.empty());
    } else {
      // There are prefixes in the Trie to search. So, we loop through each one in this reduced search space.
      // This is effectively an O(n) operation with n being the number of entries in prefixSubMap.
      return prefixSubMap.keySet().stream()
        .filter(val -> val.length() <= (destinationAddressPrefix.getValue().length() + 1)) // account for dot
        .distinct()
        // Strip trailing dot...
        .map(prefix -> prefix.substring(0, prefix.length() - 1))
        // Map to InterledgerAddressPrefix
        .map(InterledgerAddressPrefix::of)
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
   * Converter from a {@link InterledgerAddressPrefix} to a {@link String} that can be stored in the PatriciaTrie.
   * InterledgerAddressPrefix does not allow a trailing dot (.) character, but the trie requires it for proper
   * functionality.
   *
   * @param addressPrefix
   *
   * @return
   */
  protected String toTrieKey(InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);

    return addressPrefix.getValue() + ".";
  }

  private String stripTrailingDot(final String addressPrefix) {
    if (addressPrefix.endsWith(".")) {
      return addressPrefix.substring(0, addressPrefix.length() - 1);
    } else {
      return addressPrefix;
    }
  }
}
