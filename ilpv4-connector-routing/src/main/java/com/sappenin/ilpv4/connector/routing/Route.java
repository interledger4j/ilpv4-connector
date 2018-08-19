package com.sappenin.ilpv4.connector.routing;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.List;

/**
 * <p>An path for a particular payment, as seen by a remote node.</p>
 */
public interface Route {

  /**
   * <p>An {@link InterledgerAddress} representing the node that should be listed as the recipient of any next-hop
   * ledger transfer.</p>
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getNextHopAccount();

  /**
   * A list of nodes that a payment will traverse in order for a payment to make it to its destination.
   *
   * @return
   */
  List<InterledgerAddress> getPath();

  /**
   * Bytes that can be used for authentication of a given route.
   *
   * @return
   */
  byte[] getAuth();

  /**
   * An abstract implementation of {@link Route} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractRoute implements Route {


  }
}
