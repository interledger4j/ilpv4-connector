package com.sappenin.interledger.ilpv4.connector.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

/**
 * A statically configured route. Static routes take precedence over the same or shorter prefixes that are local or
 * published by peers. More specific prefixes will still take precedence.
 */
public interface StaticRoute {

  /**
   * A target address prefix that corresponds to {@code #getPeerAddress}.
   *
   * @return
   */
  InterledgerAddressPrefix getTargetPrefix();


  /**
   * The ILP address of the peer this route should route through.
   *
   * @return
   */
  InterledgerAddress getPeerAddress();

}
