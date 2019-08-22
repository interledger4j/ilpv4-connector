package org.interledger.connector.links.filters;

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

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param destinationAccountSettings The destination {@link AccountSettings} that this outgoing Prepare packet is
   *                                   being processed on.
   * @param destinationPreparePacket   The outgoing {@link InterledgerPreparePacket} that will be forwarded on this link
   *                                   (this is the packet as adjusted by the packet-switch containing, so it will
   *                                   contain the proper units and expiry for the outbound Link associated to this
   *                                   filter).
   * @param filterChain                The {@link LinkFilterChain} that this filter is operating inside of.
   *
   * @return The ILP response packet as returned by the outbound peer.
   */
  InterledgerResponsePacket doFilter(
    AccountSettings destinationAccountSettings,
    InterledgerPreparePacket destinationPreparePacket,
    LinkFilterChain filterChain
  );

}
