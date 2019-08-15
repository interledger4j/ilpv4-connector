package com.sappenin.interledger.ilpv4.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * This filter-chain provides the developer a view into the invocation chain of a filtered send-data request. Filters
 * use the this contract to invoke the next filter in the chain, or if the calling filter is the last filter in the
 * chain, to invoke the send-data call on the targeted plugin resource at the end of the chain.
 *
 * @see LinkFilter
 **/
public interface LinkFilterChain {

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param destinationAccountSettings The source {@link AccountSettings} that this outgoing Prepare packet is being
   *                                   processed for.
   * @param destinationPreparePacket   The outgoing {@link InterledgerPreparePacket} about to be processed.
   *
   * @return An optionally-present {@link InterledgerResponsePacket}.
   */
  InterledgerResponsePacket doFilter(
    AccountSettings destinationAccountSettings, InterledgerPreparePacket destinationPreparePacket
  );

}
