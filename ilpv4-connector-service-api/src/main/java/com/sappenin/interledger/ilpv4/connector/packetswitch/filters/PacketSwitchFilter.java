package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * <p>A PacketSwitch filter performs filtering tasks on a request to send data, or on the response from a target link,
 * or both.</p>
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
