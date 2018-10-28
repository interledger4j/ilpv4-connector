package com.sappenin.ilpv4.packetswitch.filters;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.util.concurrent.CompletableFuture;


/**
 * An implementation of {@link SendDataFilter} for handling balances between two accounts/plugins in an ILP connector.
 */
public class BalanceIlpPacketFilter implements SendDataFilter {

  @Override
  public CompletableFuture<InterledgerFulfillPacket> doFilter(
    final InterledgerAddress sourceAccountAddress,
    final InterledgerPreparePacket sourcePreparePacket,
    final SendDataFilterChain filterChain
  ) throws InterledgerProtocolException {

    // For now, this is just a pass-through.
    // TODO: Implement Balance tracking logic.

    // Call the next filter in the chain...
    return filterChain.doFilter(sourceAccountAddress, sourcePreparePacket);
  }
}
