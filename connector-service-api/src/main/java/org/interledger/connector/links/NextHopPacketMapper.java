package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;

/**
 * Determines how to link one account to another for purposes of routing.
 */
public interface NextHopPacketMapper {

  /**
   * Construct an instance of {@link NextHopInfo} that contains the <tt>next-hop</tt> ILP prepare packet (i.e., a new
   * packet with potentially new pricing, destination, and expiry characteristics) and the "next hop" account that the
   * new packet should be forwarded to in order to continue the Interledger protocol.
   *
   * @param sourceAccountId       The {@link AccountId} of the peer who sent this packet into the Connector. This is
   *                              typically the remote peer-address configured in a link.
   * @param incomingPreparePacket The {@link InterledgerPreparePacket} that was received from the source account on its
   *                              incoming Link.
   *
   * @return A {@link InterledgerPreparePacket} that can be sent to the next-hop.
   */
  NextHopInfo getNextHopPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket incomingPreparePacket
  ) throws RuntimeException;

}
