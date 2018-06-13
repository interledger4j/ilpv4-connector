package com.sappenin.ilpv4.model;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An account tracks a balance between two Interledger peers.
 */
@Value.Immutable
public interface Peer {

  /**
   * The ILP Address for this peer.
   *
   * @return A {@link InterledgerAddress} identifying the peer
   */
  // TODO: Convert to ILP Address once https://github.com/hyperledger/quilt/issues/139 is fixed to relax the ILP
  // address.
  String getInterledgerAddress();

  /**
   * The ILP Address for the Connector running
   * @return
   */
  String getConnectorInterledgerAddress();

  /**
   * The relationship between this account and the local node. When an Interledger node peers with another node through
   * an account, the source peer will establish a relationship that can have one of three types depending on how it fits
   * into the wider network hierarchy.
   *
   * @return An {@link PeerType}.
   */
  PeerType getRelationship();

  /**
   * All accounts associated with this Peer.
   *
   * @return
   */
  List<Account> getAccounts();

  default Optional<Account> getAccountById(final String interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");
    return this.getAccounts().stream()
      .filter(account -> account.getInterledgerAddress().equals(interledgerAddress))
      .findFirst();
  }
}
