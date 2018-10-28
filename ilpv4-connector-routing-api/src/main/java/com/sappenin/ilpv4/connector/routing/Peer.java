package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.model.IlpRelationship;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

/**
 * A container for holding information about a routing relationship between two ILPv4 nodes. Peers exchange routing
 * information, and theoretically could hold multiple accounts between themselves, although this implementation only
 * ever has a single account for a given peer.
 */
@Value.Immutable
public interface Peer {

  /**
   * The ILP Address for this peer.
   *
   * @return A {@link InterledgerAddress} identifying the peer
   */
  // InterledgerAddress getInterledgerAddress();

  IlpRelationship getIlpRelationship();

  CcpSender getCcpSender();

  CcpReceiver getCcpReceiver();

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
  //    //    .map(lpi2 -> { lpi2.getPluginType() == LOCAL})
  //    //getEntry().getaccounts.getInfo(accountId).relation : 'local'
  //  }
}
