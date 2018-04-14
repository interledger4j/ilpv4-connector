package com.sappenin.ilpv4.peer;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sappenin.ilpv4.model.Peer;
import com.sappenin.ilpv4.model.PeerId;
import com.sappenin.ilpv4.model.PeerType;
import com.sappenin.ilpv4.model.PluginType;
import com.sappenin.ilpv4.plugins.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An implementation of {@link PeerManager} that loads peers from a properties file.
 */
public class PropertyBasedPeerManager implements PeerManager {

  private final Set<Peer> peers;
  private final Map<PeerId, Plugin> plugins;
  private Optional<Peer> parentPeer = Optional.empty();

  public PropertyBasedPeerManager() {
    this.peers = Sets.newConcurrentHashSet();
    this.plugins = Maps.newConcurrentMap();
  }

  @Override
  public Optional<Peer> getParentPeer() {
    return this.parentPeer;
  }

  @Override
  public void add(Peer peer) {
    Objects.requireNonNull(peer);
    // Set the parent-peer, but only if it hasn't been set.
    if (peer.getRelationship() == PeerType.PARENT && !parentPeer.isPresent()) {
      // Set the parent peer, if it exists.
      this.parentPeer = Optional.of(peer);
    } else {
      if (peers.contains(peer)) {
        throw new RuntimeException(
          String.format("Peers may only be configured once! Peer: %s", peer));
      } else {
        peers.add(peer);
      }
    }

    // Add the proper plugin for this peer.
    peer.getAccounts().forEach(account -> {
      this.constructNewPlugin(account.getPluginType());
    });

    // Attempt to connect to the Peer.
    this.getPlugin(peer.getPeerId()).doConnect();
  }

  /**
   *
   * @param pluginType
   * @return
   */
  private Plugin constructNewPlugin(final PluginType pluginType) {
    Objects.requireNonNull(pluginType);

    switch (pluginType) {
      case BTP: {

      }
      default: {
        throw new RuntimeException(String.format("Unsupported PluginType: %s", pluginType));
      }
    }

  }

  @Override
  public void remove(final PeerId peerId) {
    this.peers.remove(peerId);
  }

  @Override
  public Optional<Peer> getPeer(final PeerId peerId) {
    return Optional.empty();
  }

  @Override
  public Stream<Peer> stream() {
    return peers.stream();
  }

  @Override
  public Plugin getPlugin(final PeerId peerId) {
    Objects.requireNonNull(peerId);

    return Optional.ofNullable(this.plugins.get(peerId))
      .orElseThrow(() -> new RuntimeException(String.format("No Plugin found for peerId: %s", peerId)));
  }
}
