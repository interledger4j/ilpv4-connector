package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerPreparePacket;

import java.math.BigDecimal;

/**
 * Determines how to link one account to another for purposes of routing.
 */
public interface NextHopPacketMapper {

  /**
   * Construct an instance of {@link NextHopInfo} that contains the <tt>next-hop</tt> ILP prepare packet (i.e., a new
   * packet with potentially new pricing, destination, and expiry characteristics) and the "next hop" account that the
   * new packet should be forwarded to in order to continue the Interledger protocol.
   *
   * @param sourceAccountSettings The {@link AccountSettings} of the peer who sent this packet into the Connector. This
   *                              is typically the remote peer-address configured in a link.
   * @param incomingPreparePacket The {@link InterledgerPreparePacket} that was received from the source account on its
   *                              incoming Link.
   *
   * @return A {@link InterledgerPreparePacket} that can be sent to the next-hop.
   */
  NextHopInfo getNextHopPacket(
    final AccountSettings sourceAccountSettings, final InterledgerPreparePacket incomingPreparePacket
  ) throws RuntimeException;

  /**
   * FIXME Only exists for shadow net testing; remove after
   * @param sourceAccountSettings
   * @param destinationAccountSettings
   * @param sourcePacket
   * @return
   */
  @Deprecated
  BigDecimal determineExchangeRate(final AccountSettings sourceAccountSettings,
                                   final AccountSettings destinationAccountSettings,
                                   final InterledgerPreparePacket sourcePacket);
}
