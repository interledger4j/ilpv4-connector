package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * This filter-chain provides the developer a view into the invocation chain of a filtered send-data request. Filters
 * use the this contract to invoke the next filter in the chain, or if the calling filter is the last filter in the
 * chain, to invoke the send-data call on the targeted plugin resource at the end of the chain.
 *
 * @see PacketSwitchFilter
 **/
public interface PacketSwitchFilterChain {

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param sourceAccountId     The source {@link AccountId} that this Prepare packet is being processed for.
   * @param sourcePreparePacket The {@link InterledgerPreparePacket} about to be processed.
   *
   * @return An optionally-present ILP response packet.
   */
  InterledgerResponsePacket doFilter(AccountId sourceAccountId, InterledgerPreparePacket sourcePreparePacket);

}
