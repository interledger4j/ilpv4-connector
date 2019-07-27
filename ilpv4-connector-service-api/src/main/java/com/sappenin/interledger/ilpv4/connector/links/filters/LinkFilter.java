package com.sappenin.interledger.ilpv4.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * A filter is an object that performs filtering tasks on either the request to send a packet, or on the response, or
 * both.
 *
 * <p>Filters perform filtering in the <code>doFilter</code> method.
 */
public interface LinkFilter {

  // TODO: Init and Destroy?

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param destinationAccountSettings The source {@link AccountSettings} that this outgoing Prepare packet is being
   *                                   processed for.
   * @param destinationPreparePacket   The outgoing {@link InterledgerPreparePacket} that should be sent to the Link
   *                                   (this is the packet adjusted by the packet-switch containing the proper units and
   *                                   expiry for the outbound Link associated to this filter).
   * @param filterChain                The {@link LinkFilterChain} that this filter is operating inside of.
   *
   * @return An optionally-present ILP response packet.
   */
  InterledgerResponsePacket doFilter(
    AccountSettings destinationAccountSettings,
    InterledgerPreparePacket destinationPreparePacket,
    LinkFilterChain filterChain
  );

}
