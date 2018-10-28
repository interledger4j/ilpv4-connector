package com.sappenin.ilpv4.connector.routing;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.List;

import static com.sappenin.ilpv4.connector.routing.Route.EMPTY_AUTH;

/**
 * Models an incoming getRoute received from a particular peer.
 */
@Value.Immutable
public interface IncomingRoute extends BaseRoute {

  /**
   * <p>An {@link InterledgerAddressPrefix} representing the address of the peer that sent this getRoute to us.</p>
   *
   * @return A {@link InterledgerAddress}.
   */
  InterledgerAddress getPeerAddress();

  /**
   * A list of nodes that a payment will traverse in order for a payment to make it to its destination.
   *
   * @return
   */
  List<InterledgerAddress> getPath();

  /**
   * Bytes that can be used for authentication of a given getRoute. Reserved for the future, currently not used.
   *
   * @return
   */
  @Value.Default
  default byte[] getAuth() {
    return EMPTY_AUTH;
  }

}