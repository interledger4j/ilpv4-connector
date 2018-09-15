package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.model.Account;
import org.interledger.core.InterledgerAddress;

import java.util.Optional;

/**
 * Manages Peers in an ILPv4 Connector. Peering occurs when two connectors initiate a communications channel using a
 * single {@link Account} communicating over a single {@link Peer}.
 *
 * @deprecated Think about this manager - this really feels like an internal concern of the RoutingService, so maybe
 * it should just go there?
 */
@Deprecated
public interface PeerManager {

  /**
   * Associate the supplied {@code lpi2} to the supplied {@code accountAddress}.
   *
   * @param accountAddress
   * @param peer
   */
  void setPeer(InterledgerAddress accountAddress, Peer peer);

  /**
   * <p>Retrieve a {@link Peer} for the supplied {@code accountAddress}.</p>
   *
   * <p>Note that this method returns one or zero peers using an exact-match algorithm on the address because a
   * particular account can have only one peer at a time.</p>
   *
   * @param peerAccountAddress The {@link InterledgerAddress} of the remote peer.
   *
   * @return An optinoally-present {@link Peer}.
   */
  Optional<Peer> getPeer(InterledgerAddress peerAccountAddress);

}
