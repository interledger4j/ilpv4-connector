package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * <p>A PacketSwitch filter is an object that performs filtering tasks on either a request to send money, or on the
 * response  from a target plugin, or both.</p>
 *
 * <p>Filters perform filtering in the <code>doFilter</code> method.
 */
public interface PacketSwitchFilter {

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param sourceAccountSettings The source {@link AccountSettings} that this Prepare packet is being processed on
   *                              behalf of.
   * @param sourcePreparePacket   The {@link InterledgerPreparePacket} about to be processed.
   * @param filterChain           The {@link PacketSwitchFilterChain} that this filter is operating inside of.
   *
   * @return An optionally-present ILP response packet.
   */
  InterledgerResponsePacket doFilter(
    AccountSettings sourceAccountSettings, InterledgerPreparePacket sourcePreparePacket, PacketSwitchFilterChain filterChain
  );

}
