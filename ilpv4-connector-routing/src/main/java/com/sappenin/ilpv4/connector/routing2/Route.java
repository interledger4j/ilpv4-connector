package com.sappenin.ilpv4.connector.routing2;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.List;

/**
 * An ILPv4 Route.
 */
public interface Route {


  List<InterledgerAddress> getPath();

  /**
   * <p>An {@link InterledgerAddress} representing the account that should be listed as the recipient of any next-hop
   * ledger transfers.</p>
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getNextHopAccount();


  byte[] getAuth();


  /**
   * An abstract implementation of {@link com.sappenin.ilpv4.connector.routing.Route} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractRoute implements com.sappenin.ilpv4.connector.routing.Route {


  }
}
