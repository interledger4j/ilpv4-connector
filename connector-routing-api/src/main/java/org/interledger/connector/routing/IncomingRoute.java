package org.interledger.connector.routing;

import static org.interledger.connector.routing.Route.EMPTY_AUTH;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import org.immutables.value.Value;

import java.util.List;

/**
 * Models an incoming route received from a particular peer.
 */
@Value.Immutable
public interface IncomingRoute extends BaseRoute {

  /**
   * <p>An {@link AccountId} representing the peer account that sent this route to us.</p>
   *
   * @return A {@link AccountId}.
   */
  AccountId peerAccountId();

  /**
   * A list of nodes (represented by {@link InterledgerAddress} that a payment will traverse in order for a payment to
   * make it to its destination.
   *
   * @return A {@link List} of type {@link InterledgerAddress}.
   */
  List<InterledgerAddress> path();

  /**
   * Bytes that can be used for authentication of a given route. Reserved for the future, currently not used.
   *
   * @return
   */
  @Value.Default
  default byte[] auth() {
    return EMPTY_AUTH;
  }

}
