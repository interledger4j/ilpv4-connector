package com.sappenin.ilpv4.peer;

import com.sappenin.ilpv4.model.Peer;
import com.sappenin.ilpv4.model.PeerId;
import com.sappenin.ilpv4.plugins.Plugin;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Manages peer relationships.</p>
 *
 * <p>
 * A connector might have multiple accounts with the same peer, each of which may have a different or identical currency
 * code and may communicate over the same or different network connections (e.g., BTP on USD and EUR on gRPC or REST).
 * </p>
 */
public interface PeerManager {

  /**
   * An optionlly-present parent peer. //TODO: Add pointer to an overview of parent/child/peer relationships.
   */
  Optional<Peer> getParentPeer();

  /**
   * Add a peer peer to this manager.
   */
  void add(Peer peer);

  /**
   * Remove an peer from this manager by its id.
   */
  void remove(PeerId peerId);

  /**
   * Get the Ledger Layer2Plugin for the specified {@code ledgerPrefix}.
   *
   * @param peerId The {@link PeerId} of the peer to retrieve. ¬   * @return The requested {@link Peer}, if present.
   */
  Optional<Peer> getPeer(PeerId peerId);

  /**
   * Creates a {@code Stream} of Peers.
   */
  Stream<Peer> stream();

  /**
   * Gets the {@link Plugin} for the specified peer id.
   */
  Plugin getPlugin(PeerId peerId);

  // TODO: Add getAccounts().

}
