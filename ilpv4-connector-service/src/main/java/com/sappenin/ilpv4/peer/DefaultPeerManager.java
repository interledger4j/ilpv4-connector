package com.sappenin.ilpv4.peer;

import com.google.common.collect.Maps;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.model.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An implementation of {@link PeerManager} that loads peer configurations that are ultimately sourced from a properties
 * file.
 */
public class DefaultPeerManager implements PeerManager {

  private final Map<InterledgerAddress, Peer> peers = Maps.newConcurrentMap();

  private final AccountManager accountManager;

  private Optional<Peer> parentPeer = Optional.empty();

  public DefaultPeerManager(final AccountManager accountManager) {
    this.accountManager = Objects.requireNonNull(accountManager);
  }

  /**
   * Called just before this Peer Manager will be destroyed (i.e., disconnect all peers).
   */
  @Override
  public void shutdown() {
    // Attempt to disconnect from each Account configured for this Peer.
    this.peers.values().stream().forEach(this::disconnectPeer);
  }

  @Override
  public void add(final Peer peer) {
    Objects.requireNonNull(peer);

    try {
      // Set the parent-peer, but only if it hasn't been set.
      if (peer.getRelationship() == PeerType.PARENT && !parentPeer.isPresent()) {
        // Set the parent peer, if it exists.
        this.parentPeer = Optional.of(peer);
      } else {
        if (peers.putIfAbsent(peer.getInterledgerAddress(), peer) != null) {
          throw new RuntimeException(
            String.format("Peers may only be configured once! Peer: %s", peer));
        }
      }

      // Connect each account in the peer...
      peer.getAccounts().stream()
        .map(Account::getInterledgerAddress)
        .map(accountManager::getPlugin)
        .forEach(Plugin::doConnect);
    } catch (Exception e) {
      // If any exception is thrown, then remove the peer and any plugins...
      this.remove(peer.getInterledgerAddress());
    }
  }

  @Override
  public void remove(final InterledgerAddress interledgerAddress) {
    // Disconnect all Accounts and then remove the peer from teh collection.
    this.getPeer(interledgerAddress).ifPresent(this::disconnectPeer);
    this.peers.remove(interledgerAddress);
  }

  @Override
  public Optional<Peer> getPeer(final InterledgerAddress interledgerAddress) {
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

  /**
   * Helper method to disconnect a {@link Peer} based upon its Interledger Address.
   *
   * @param peer The {@link Peer} to disconnect.
   */
  private void disconnectPeer(final Peer peer) {
    Objects.requireNonNull(peer);
    peer.getAccounts().stream()
      .map(Account::getInterledgerAddress)
      .map(accountManager::getPlugin)
      .forEach(Plugin::doConnect);
  }
}
