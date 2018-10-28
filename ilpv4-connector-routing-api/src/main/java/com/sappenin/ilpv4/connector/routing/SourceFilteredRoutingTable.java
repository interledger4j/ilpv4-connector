package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Optional;

/**
 * <p>An extension of {@link RoutingTable} that allows for next-hop lookups to be filtered by a source address so
 * that appropriate next-hops can be returned depending on who is sending a packet.</p>
 */
public interface SourceFilteredRoutingTable<R extends SourceRestrictedRoute> extends RoutingTable<R> {


  /**
   * Given a final destination ILP address, determine the "best" getRoute that an ILP payment message or should
   * traverse.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   * @param sourcePrefix            An {@link InterledgerAddress} representing the incoming ILP prefix of the node that
   *                                sent the payment or message. Used to filter next-hop routes by a source address
   *                                based upon the attributes of each getRoute.
   *
   * @return An optionally-present {@link R} for the supplied addresses.
   */
  Optional<R> findNextHopRoute(
    final InterledgerAddress finalDestinationAddress, final InterledgerAddressPrefix sourcePrefix
  );

  // TODO: Consider removing this if it's not used. In order to support this functionality, we would need a
  // multi-prefix Map (which exists), so the routing table interface would stay the same (always return a single
  // route) but allow the underlying implementation to configure multiple routing-table entries, and then always
  // return the best one, possibly via this method?

  //    ) {
  //      Objects.requireNonNull(finalDestinationAddress);
  //      Objects.requireNonNull(sourcePrefix);
  //
  //      // NOTE: This is not very performant since all routes are loaded and then post-filtered. However, as a default
  //      // implementation, this is sufficient, but implementations of this interface should improve upon this method's
  //      // implementation.
  //      return this.findNextHopRoute(finalDestinationAddress).stream()
  //        // Only return routes that are allowed per the source prefix filter...
  //        .filter(route -> route.getSourcePrefixRestrictionRegex().matcher(sourcePrefix.getValue()).matches())
  //        .collect(Collectors.toList());
  //    }

}
