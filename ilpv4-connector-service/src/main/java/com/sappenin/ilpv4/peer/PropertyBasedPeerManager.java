package com.sappenin.ilpv4.peer;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.model.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An implementation of {@link PeerManager} that loads peers from a properties file.
 */
public class PropertyBasedPeerManager implements PeerManager {

  private final Map<PeerId, Peer> peers;
  private Optional<Peer> parentPeer = Optional.empty();

  public PropertyBasedPeerManager() {
    this.peers = Maps.newConcurrentMap();
  }

  /**
   * Called just before this Peer Manager will be destroyed (i.e., disconnect all peers).
   */
  @Override
  public void shutdown() {
    // Attempt to connect to each Account configured for this Peer.
    this.peers.values().stream().forEach(peer -> peer.getAccounts().stream()
      .map(Account::getPlugin)
      .forEach(Plugin::doDisconnect)
    );
  }

  @Override
  public void add(final Peer peer) {
    Objects.requireNonNull(peer);
    // Set the parent-peer, but only if it hasn't been set.
    if (peer.getRelationship() == PeerType.PARENT && !parentPeer.isPresent()) {
      // Set the parent peer, if it exists.
      this.parentPeer = Optional.of(peer);
    } else {
      if (peers.putIfAbsent(peer.getPeerId(), peer) != null) {
        throw new RuntimeException(
          String.format("Peers may only be configured once! Peer: %s", peer));
      }
    }

    // Connect each account...
    peer.getAccounts().stream()
      .map(Account::getPlugin)
      .forEach(Plugin::doConnect);
  }

  @Override
  public void remove(final PeerId peerId) {
    // Disconnect all Accounts...
    Optional.ofNullable(this.peers.get(peerId))
      .ifPresent(peer -> peer.getAccounts().stream()
        .map(Account::getPlugin)
        .forEach(Plugin::doDisconnect)
      );
    this.peers.remove(peerId);
  }

  @Override
  public Optional<Peer> getPeer(final PeerId peerId) {
    return Optional.empty();
  }

  @Override
  public Stream<Peer> stream() {
    return peers.values().stream();
  }

  @Override
  public Optional<Peer> getParentPeer() {
    return this.parentPeer;
  }

}
