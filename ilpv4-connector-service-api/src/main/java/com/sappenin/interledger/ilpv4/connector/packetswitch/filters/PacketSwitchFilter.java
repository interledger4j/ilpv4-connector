package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * A filter is an object that performs filtering tasks on either the request to send money, or on the response from a
 * target plugin, or both.
 *
 * <p>Filters perform filtering in the <code>doFilter</code> method.
 */
public interface PacketSwitchFilter {

  // TODO: Init and Destroy?

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param sourceAccountId     The source {@link AccountId} that this Prepare packet is being processed for.
   * @param sourcePreparePacket The {@link InterledgerPreparePacket} about to be processed.
   * @param filterChain         The {@link PacketSwitchFilterChain} that this filter is operating inside of.
   *
   * @return An optionally-present ILP response packet.
   */
  InterledgerResponsePacket doFilter(
    // TODO: Consider using AccountSettings instead of AccountId in this interface to avoid AccountManager lookups.
    AccountId sourceAccountId, InterledgerPreparePacket sourcePreparePacket, PacketSwitchFilterChain filterChain
  );

}
