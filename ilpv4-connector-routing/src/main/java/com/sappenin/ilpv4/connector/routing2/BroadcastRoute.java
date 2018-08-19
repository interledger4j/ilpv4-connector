package com.sappenin.ilpv4.connector.routing2;

import com.sappenin.ilpv4.InterledgerAddressPrefix;
import com.sappenin.ilpv4.connector.routing.Route;
import org.immutables.value.Value;

/**
 * An ILPv4 "Incoming" Route (i.e., routing information from a remote peer).
 */
public interface BroadcastRoute extends Route {

  /**
   * The ILP address prefix that this route applies to.
   *
   * @return
   */
  InterledgerAddressPrefix getPrefix();

  /**
   * An abstract implementation of {@link BroadcastRoute} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractBroadcastRoute implements BroadcastRoute {


  }
}
