package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Optional;
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
 *     <li>The <tt>Origin</tt> of a route is the connector who first advertised the route.</li>
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
 * │   g.a   │─ ─(1)│   g.b   │(2)- ─│   g.c   │
 * └─────────┘      └─────────┘      └─────────┘
 *                      (3)
 *                       │
 *                  ┌─────────┐
 *                  │   g.d   │
 *                  └─────────┘
 * </pre>
 *
 * <p>The following is an example routing table from the perspective of <tt>g.b</tt>:<p>
 *
 * <pre>
 *  | targetPrefix | nextHopAccount | sourcePrefixFilter |
 *  |==============|================|====================|
 *  | g.a          | 1              | [absent            |
 *  | g.c          | 2              | [absent]           |
 *  | g.d          | 3              | g.c.(.*?)          |
 *  | g   	       | 1              | g.b.(.*?)          |
 * </pre>
 *
 * <p> In the above example, the route returned for a final-destination address of `g.a` would contain account `1`,
 * the route returned for a final-destination address of `g.c` would contain account `2`, and the final-destination
 * address of `g.d` would be sent through account `3`, but only if the payment originates from account `2`.</p>
 *
 * <p>Last but not least, the above table has a global catch-all route for the `g.` prefix, which will return a
 * "next-hop" account of `1` for any routing requests that don't match any other prefix in the table.</p>
 *
 * <p>Using data from a routing table allows an ILP node to forward packets to the correct next-hop in an overall
 * payment path, without holding the entire topology of all ILP nodes in memory.</p>
 *
 * <p>This interface is extensible in that it can hold simple routes of type {@link BaseRoute}, or it can hold
 * more complicated implementations that extend {@link BaseRoute}.</p>
 */
public interface RoutingTable<R extends BaseRoute> {

  InterledgerAddressPrefix SELF_INTERNAL = InterledgerAddressPrefix.SELF.with("internal");
  InterledgerAddress STANDARD_DEFAULT_ROUTE = InterledgerAddress.of(SELF_INTERNAL.getValue());

  /**
   * Add a route to this routing table. If the route already exists (keyed by {@code prefix}, then this operation is a
   * no-op.
   *
   * @param route A {@link R} to add to this routing table.
   */
  R addRoute(R route);

  /**
   * Remove a particular route from the routing table, based upon its target prefix.
   *
   * @param routePrefix The address prefix that uniquely identifies the route to removeEntry from this routing table.
   */
  Optional<R> removeRoute(InterledgerAddressPrefix routePrefix);

  /**
   * Accessor for all Routes in this routing table that are keyed by a target-prefix. Unlike {@link
   * #findNextHopRoute(InterledgerAddress)} or {@link #findNextHopRoute(InterledgerAddress)}, this method does not do
   * any "longest-prefix" matching, and is instead meant to provide getEntry-by-key semantics for the routing table.
   *
   * @param addressPrefix An {@link InterledgerAddressPrefix} prefix used as a key in the routing table.
   *
   * @return An {@link Optional} route for the supplied {@code addressPrefix} key.
   */
  Optional<R> getRouteByPrefix(InterledgerAddressPrefix addressPrefix);

  /**
   * Obtain a view of all Routes in this Routing Table.
   *
   * @return
   */
  Iterable<InterledgerAddressPrefix> getAllPrefixes();

  /**
   * Perform the following action on each item in the routing table.
   *
   * @param action A {@link BiConsumer} that is applied to all routes in this table. The BiConsumer accepts an {@link
   *               InterledgerAddressPrefix} and an instance of {@link R}.
   */
  void forEach(final BiConsumer<InterledgerAddressPrefix, R> action);

  /**
   * Determine the next-hop route for the specified {@code interledgerAddress}, if any.
   *
   * @param interledgerAddress An {@link InterledgerAddress} representing the final destination of a packet (i.e., the
   *                           address of the receiver of an ILP payment).
   *
   * @return An optionally-present {@link R}.
   */
  Optional<R> findNextHopRoute(InterledgerAddress interledgerAddress);

  /**
   * Reset the routing table to an empty state.
   */
  void reset();
}
