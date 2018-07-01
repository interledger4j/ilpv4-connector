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

  private final Map<InterledgerAddress, Peer> peers;
  private final AccountManager accountManager;

  private Optional<Peer> parentPeer = Optional.empty();

  public DefaultPeerManager(final AccountManager accountManager) {
    this.peers = Maps.newConcurrentMap();
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

    // Remove this peer
    this.remove(peer.getInterledgerAddress());

    try {
      // Set the parent-peer, but only if it hasn't been set.
      if (peer.getRelationship() == PeerType.PARENT && !parentPeer.isPresent()) {
        // Set the parent peer, if it exists.
        this.setParentPeer(peer);
      } else {
        if (peers.putIfAbsent(peer.getInterledgerAddress(), peer) != null) {
          throw new RuntimeException(
            String.format("Peers may only be configured once! Peer: %s", peer));
        }
      }

      // Connect each account in the peer...
      // For BTP, we only need a single server to handle all peers and all account in a peer.
      peer.getAccounts().stream().forEach(account -> {
        // Add every account to the account manager...
        accountManager.add(account);
      });

      accountManager.stream()
        .map(Account::getInterledgerAddress)
        .map(accountManager::getPlugin)
        .forEach(Plugin::doConnect);

    } catch (RuntimeException e) {
      // If any exception is thrown, then remove the peer and any plugins...
      this.remove(peer.getInterledgerAddress());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void remove(final InterledgerAddress interledgerAddress) {
    this.getPeer(interledgerAddress).ifPresent(peer -> {
      // Disconnect all Accounts for the Peer.
      this.disconnectPeer(peer);

      if (peer.getRelationship() == PeerType.PARENT) {
        // Set the parent peer, if it exists.
        this.parentPeer = Optional.empty();
      }
    });

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
   * Allows only a single thread to set a peer at a time, ensuring that only one will win.
   *
   * @param peer
   */
  private synchronized void setParentPeer(final Peer peer) {
    Objects.requireNonNull(peer);

    if (this.parentPeer.isPresent()) {
      throw new RuntimeException("Only a single Parent Peer may be configured for a Connector!");
    }

    this.parentPeer = Optional.of(peer);
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

  @Override
  public AccountManager getAccountManager() {
    return accountManager;
  }
}
