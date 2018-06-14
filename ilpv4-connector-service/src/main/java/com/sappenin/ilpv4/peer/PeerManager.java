package com.sappenin.ilpv4.peer;

import com.sappenin.ilpv4.model.Peer;

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
   * Called just before this Peer Manager will be destroyed (i.e., disconnect all peers).
   */
  void shutdown();

  /**
   * Add a peer peer to this manager.
   */
  void add(Peer peer);

  /**
   * Remove an peer from this manager by its id.
   */
  void remove(String interledgerAddress);

  /**
   * Get the Ledger Layer2Plugin for the specified {@code ledgerPrefix}.
   *
   * @param interledgerAddress The {@link String} of the peer to retrieve.
   *
   * @return The requested {@link Peer}, if present.
   */
  Optional<Peer> getPeer(String interledgerAddress);

  /**
   * Creates a {@code Stream} of Peers.
   */
  Stream<Peer> stream();
}
