package com.sappenin.ilpv4.connector.routing2;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.List;

/**
 * An ILPv4 "Incoming" Route (i.e., routing information from a remote peer).
 */
public interface IncomingRoute {

  InterledgerAddress getPeer();

  InterledgerAddress getPrefix();

  List<InterledgerAddress> getPath();

  byte[] getAuth();

  /**
   * An abstract implementation of {@link com.sappenin.ilpv4.connector.routing.Route} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractIncomingRoute implements IncomingRoute {


  }
}
