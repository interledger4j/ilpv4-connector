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
  NextHopInfo getNextHopPacket(AccountSettings sourceAccountSettings, InterledgerPreparePacket incomingPreparePacket)
    throws RuntimeException;

  /**
   * NOTE: This method only exists in order to get FX info into PubSub + BigQuery. See deprecation note below for more
   * details.
   *
   * @param sourceAccountSettings
   * @param destinationAccountSettings
   * @param sourcePacket
   *
   * @return
   *
   * @deprecated This only exists to faciliate getting FX information into BigQuery. However, this method should be
   *   removed once https://github.com/interledger4j/ilpv4-connector/issues/529 is fixed (see that issue for more
   *   details).
   */
  @Deprecated
  BigDecimal determineExchangeRate(
    AccountSettings sourceAccountSettings,
    AccountSettings destinationAccountSettings,
    InterledgerPreparePacket sourcePacket);
}
