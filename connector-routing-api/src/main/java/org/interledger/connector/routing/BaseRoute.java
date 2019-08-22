package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

/**
 * <p>The most primitive type of object that can be stored in an ILPv4 {@link RoutingTable}.
 */
public interface BaseRoute {

  /**
   * <p>An {@link InterledgerAddressPrefix} used as the lookup key in a routing table, in order to perform a
   * longest-prefix match operation against a final destination payment address. For example, if a payment is destined
   * for <tt>g.example.alice</tt>, then the longest-matching target prefix might be <tt>g.example</tt>, assuming such an
   * entry exists in the routing table.</p>
   *
   * @return A {@link InterledgerAddressPrefix}.
   */
  InterledgerAddressPrefix getRoutePrefix();
}
