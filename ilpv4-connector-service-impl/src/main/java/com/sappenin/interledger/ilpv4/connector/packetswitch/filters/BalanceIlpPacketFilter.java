package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;


/**
 * An implementation of {@link PacketSwitchFilter} for handling balances between two accounts/links in an ILP
 * connector.
 */
public class BalanceIlpPacketFilter implements PacketSwitchFilter {

  @Override
  public Optional<InterledgerResponsePacket> doFilter(
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
