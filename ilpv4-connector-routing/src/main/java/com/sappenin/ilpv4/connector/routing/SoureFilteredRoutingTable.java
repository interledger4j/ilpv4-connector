package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>An extension of {@link RoutingTable} that allows for next-hop lookups to be filtered by a source address so
 * that appropriate next-hops can be returned depending on who is sending a packet.</p>
 */
public interface SoureFilteredRoutingTable<R extends SourceRestrictedRoute> extends RoutingTable<R> {


  /**
   * Given a final destination ILP address, determine the "best" getRoute that an ILP payment message or should traverse.
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
  default Collection<R> findNextHopRoutes(
    final InterledgerAddress finalDestinationAddress, final InterledgerAddressPrefix sourcePrefix
  ) {
    Objects.requireNonNull(finalDestinationAddress);
    Objects.requireNonNull(sourcePrefix);

    // NOTE: This is not very performant since all routes are loaded and then post-filtered. However, as a default
    // implementation, this is sufficient, but implementations of this interface should improve upon this method's
    // implementation.
    return this.findNextHopRoutes(finalDestinationAddress).stream()
      // Only return routes that are allowed per the source prefix filter...
      .filter(route -> route.getSourcePrefixRestrictionRegex().matcher(sourcePrefix.getValue()).matches())
      .collect(Collectors.toList());
  }

}
