package com.sappenin.ilpv4.model;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Map;

/**
 * An account tracks a balance between two Interledger peers.
 */
@Deprecated
@Value.Immutable
public interface Peer {

  /**
   * The ILP Address for this peer.
   *
   * @return A {@link InterledgerAddress} identifying the peer
   */
 // InterledgerAddress getInterledgerAddress();

  /**
   * The relationship between this account and the local node. When an Interledger node peers with another node through
   * an account, the source peer will establish a relationship that can have one of three types depending on how it fits
   * into the wider network hierarchy.
   *
   * @return An {@link IlpRelationship}.
   */
  //IlpRelationship getRelationship();

  /**
   * All accounts associated with this Peer.
   *
   * @return
   */
  //Map<InterledgerAddress, Account> getAccounts();

//  default Optional<Account> getAccountById(final InterledgerAddress interledgerAddress) {
//    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");
//    return this.getAccounts().stream()
//      .filter(account -> account.getInterledgerAddress().equals(interledgerAddress))
//      .findFirst();
//  }

//  default IlpRelationship getAccountRelation(final InterledgerAddress accountAddress) {
//    Objects.requireNonNull(accountAddress);
//    return null;
//    //    return  this.getAccountById(accountAddress)
//    //    .map(Account::getPlugin)
//    //    .map(plugin -> { plugin.getPluginType() == LOCAL})
//    //get().getaccounts.getInfo(accountId).relation : 'local'
//  }
}
