package com.sappenin.ilpv4.connector.routing2;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

/**
 * An ILPv4 "Incoming" Route (i.e., routing information from a remote peer).
 */
public interface BroadcastRoute extends Route {

  /**
   * The ILP address prefix that this route applies to.
   *
   * @return
   */
  InterledgerAddress getPrefix();

  /**
   * An abstract implementation of {@link com.sappenin.ilpv4.connector.routing.Route} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractBroadcastRoute implements BroadcastRoute {


  }
}
