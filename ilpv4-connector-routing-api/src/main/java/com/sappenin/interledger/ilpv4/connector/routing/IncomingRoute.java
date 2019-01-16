package com.sappenin.interledger.ilpv4.connector.routing;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.List;

import static com.sappenin.interledger.ilpv4.connector.routing.Route.EMPTY_AUTH;

/**
 * Models an incoming getRoute received from a particular peer.
 */
@Value.Immutable
public interface IncomingRoute extends BaseRoute {

  /**
   * <p>An {@link AccountId} representing the peer account that sent this route to us.</p>
   *
   * @return A {@link AccountId}.
   */
  AccountId getPeerAccountId();

  /**
   * A list of nodes (represented by {@link InterledgerAddress} that a payment will traverse in order for a payment to
   * make it to its destination.
   *
   * @return A {@link List} of type {@link InterledgerAddress}.
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