package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;

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
   * @param sourceAccountId
   * @param sourcePreparePacket
   * @param filterChain
   *
   * @return
   */
  Optional<InterledgerResponsePacket> doFilter(
    AccountId sourceAccountId, InterledgerPreparePacket sourcePreparePacket, PacketSwitchFilterChain filterChain
  );

}
