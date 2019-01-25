package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * An implementation of {@link PacketSwitchFilter} for handling balances between two accounts/plugins in an ILP connector.
 */
public class BalanceIlpPacketFilter implements PacketSwitchFilter {

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    // For now, this is just a pass-through.
    // TODO: Implement Balance tracking logic.

    // Call the next filter in the chain...
    return filterChain.doFilter(sourceAccountId, sourcePreparePacket);
  }
}
