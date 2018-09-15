package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.model.RoutingTableId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * <p>A Routing Table is a lookup table of routes, indexed by prefix.</p>
 *
 * <p>
 * <pre>
 *   <ul>
 *     <li><tt>Routes</tt> are an advertisement by a connector, stating that it can reach a particular destination
 *     address prefix.</li>
 *     <li>A <tt>Prefix</tt> is an ILP address prefix covering a portion of the overall Interledger address space.</li>
 *     <li>The <tt>Origin</tt> of a getRoute is the connector who first advertised the getRoute.</li>
 *     <li>A Routing Table is a lookup table of routes, indexed by prefix.</li>
 *   </ul>
 * </pre>
 * </p>
 *
 * <p>This interface defines a lookup-table of InterledgerAddress routes used to determine a target account that
 * should be used in order to move an Interledger prepare packet to the next Connector in an Interledger payment
 * chain.</p>
 *
 * <p>To make a routing decision, this interface accepts a final-destination Interledger address, and the attempts
 * to find the longest-matching prefix to use as the next-hop. For example, imagine a network that looks like this:</p>
 *
 * <pre>
 * ┌─────────┐      ┌─────────┐      ┌─────────┐
 * │   g.a   │─ ─ ─ │   g.b   │─ ─ ─ │   g.c   │
 * └─────────┘      └─────────┘      └─────────┘
 *                       │
 *
 *                       │
 *                  ┌─────────┐
 *                  │   g.b   │
 *                  └─────────┘
 * </pre>
 *
 * <p>The following is an example routing table from the perspective of <tt>g.b</tt>:<p>
 *
 * <pre>
 *  | targetPrefix | nextHopAccount | sourcePrefixFilter |
 *  |==============|================|====================|
 *  | g.a          | g.a            | [absent            |
 *  | g.c          | g.b            | [absent]           |
 *  | g.d          | g.d            | g.c.(.*?)          |
 *  | g   	       | g.a            | g.b.(.*?)          |
 * </pre>
 *
 * <p> In the above example, the getRoute returned for a final-destination address of <tt>g.a</tt> would be
 * <tt>g.a</tt>; the getRoute returned for a final-destination address of <tt>g.c</tt> would be <tt>g.c</tt>, and the
 * final-destination address of <tt>g.d</tt> would be sent through g.d, but only if the payment originates from
 * <tt>g.c</tt>.</p>
 *
 * <p>Last but not least, the above table has a global catch-all getRoute for the "g" prefix, which will return a
 * "next-hop" address of "g.a" for any routing requests that don't match any other prefix in the table.</p>
 *
 * <p>Using data from a routing table allows an ILP node to forward packets to the correct next hop in an overall
 * payment path, without holding the entire graph of all ILP nodes in-memory.</p>
 *
 * <p> This interface is extensible in that it can hold simple routes of type {@link BaseRoute}, or it can hold
 * more complicated implementations that extend {@link BaseRoute}.</p>
 */
public interface RoutingTable<R extends BaseRoute> {

  /**
   * The unique identifier of this routing table, primarily used for coordinating Route updates via CCP.
   *
   * @return A {@link UUID}.
   */
  RoutingTableId getRoutingTableId();

  /**
   * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
   *
   * @param expectedRoutingTableId the expected value
   * @param newRoutingTableId      the new value
   *
   * @return {@code true} if successful. False return indicates that the actual value was not equal to the expected
   * value.
   */
  boolean compareAndSetRoutingTableId(RoutingTableId expectedRoutingTableId, RoutingTableId newRoutingTableId);

  /**
   * <p>Accessor for the current getEpoch of this routing table.</p>
   *
   * <p>Every time the routing table is modified, the update is logged and the revision number of the table is
   * increased. The revision number of the table is called an **getEpoch**.</p>
   *
   * @return A <tt>long</tt>.
   */
  default long getCurrentEpoch() {
    return 0L;
  }

  /**
   * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
   *
   * @param expectedEpoch the expected value
   * @param newEpoch      the new value
   *
   * @return {@code true} if successful. False return indicates that the actual value was not equal to the expected
   * value.
   */
  boolean compareAndSetCurrentEpoch(long expectedEpoch, long newEpoch);

  /**
   * Add a getRoute to this routing table. If the getRoute already exists (keyed by {@code prefix}, then this operation
   * is a no-op.
   *
   * @param route A {@link R} to add to this routing table.
   */
  boolean addRoute(InterledgerAddressPrefix routePrefix, R route);

  /**
   * Remove a particular getRoute from the routing table, based upon its target prefix.
   *
   * @param routePrefix The address prefix that uniquely identifies the getRoute to remove from this routing table.
   */
  Collection<R> removeRoute(InterledgerAddressPrefix routePrefix);

  /**
   * Accessor for all Routes in this routing table that are keyed by a target-prefix. Unlike {@link
   * #findNextHopRoutes(InterledgerAddress)} or {@link #findNextHopRoutes(InterledgerAddress)}, this method does not do
   * any "longest-prefix" matching, and is instead meant to provide get-by-key semantics for the routing table.
   *
   * @param routePrefix An {@link InterledgerAddressPrefix} prefix used as a key in the routing table.
   *
   * @return A {@link Collection} of all routes for the supplied {@code addressPrefix} key.
   */
  // TODO: See localRoutes in getRoute-broadcaster.js. It stores a single R per prefix. This interface
  // needs this ability.
  Collection<R> getRoutesByPrefix(InterledgerAddressPrefix routePrefix);

  /**
   * Remove all routes from the routing table that are keyed by {@code targetPrefix}.
   *
   * @param routePrefix An {@link InterledgerAddressPrefix} prefix used as a key in the routing table.
   */
  Collection<R> removeAllRoutesForPrefix(InterledgerAddressPrefix routePrefix);

  /**
   * Obtain a view of all Routes in this Routing Table.
   *
   * @return
   */
  Iterable<InterledgerAddressPrefix> getAllPrefixes();

  /**
   * Perform the following action on each item in the routing table.
   *
   * @param action
   */
  void forEach(final BiConsumer<? super InterledgerAddressPrefix, ? super Collection<R>> action);

  /**
   * Determine the ledger-prefix for the "next hop" ledger that a payment should be delivered/forwarded to. If this
   * routing table has no such getRoute, then return {@link Optional#empty()}.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final destination of the payment
   *                                (i.e., the address of the receiver of an ILP payment).
   *
   * @return An optionally-present ILP-prefix identifying the ledger-plugins that should be used to make the next local
   * transfer in an Interledger payment.
   */
  Collection<R> findNextHopRoutes(InterledgerAddress finalDestinationAddress);

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
  //  default Collection<R> findNextHopRoutes(
  //    final InterledgerAddress finalDestinationAddress, final InterledgerAddressPrefix sourcePrefix
  //  ) {
  //    Objects.requireNonNull(finalDestinationAddress);
  //    Objects.requireNonNull(sourcePrefix);
  //
  //    // NOTE: This is not very performant since all routes are loaded and then post-filtered. However, as a default
  //    // implementation, this is sufficient, but implementations of this interface should improve upon this method's
  //    // implementation.
  //    return this.findNextHopRoutes(finalDestinationAddress).stream()
  //      // Only return routes that are allowed per the source prefix filter...
  //      .filter(getRoute -> getRoute.getSourcePrefixRestrictionRegex().matcher(sourcePrefix.getValue()).matches())
  //      .collect(Collectors.toList());
  //  }

  /**
   * Reset the routing table to an empty state.
   */
  void reset();

  /**
   * An in-memory log of getRoute-updates, as received from remote peers. This is typed as an Array because it is
   * typically operated upon using index-based lookups, so we want to ensure that an implementation doesn't accidentally
   * use a non-performant {@link List}.
   *
   * @return
   */
  List<RouteUpdate> getRouteUpdateLog();
}
