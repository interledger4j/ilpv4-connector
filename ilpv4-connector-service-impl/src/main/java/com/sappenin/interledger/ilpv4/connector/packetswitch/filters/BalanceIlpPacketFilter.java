package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


/**
 * An implementation of {@link PacketSwitchFilter} for handling balances between two accounts/links in an ILP connector.
 * Only processes fulfillment responses.
 */
public class BalanceIlpPacketFilter implements PacketSwitchFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public Optional<InterledgerResponsePacket> doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    // TODO: JS filter applies to both pre and post!

    final Optional<InterledgerResponsePacket> ilpResponse = filterChain.doFilter(sourceAccountId, sourcePreparePacket);

    ilpResponse
      .filter(responsePacket -> InterledgerFulfillPacket.class.isAssignableFrom(responsePacket.getClass()))
      .ifPresent(interledgerFulfillPacket -> {
        // TODO: Increment Balance
        logger.info("Balance incremented by {}", sourcePreparePacket.getAmount());
      });

    return ilpResponse;
  }
}
