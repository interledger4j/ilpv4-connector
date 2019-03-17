package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for limiting per-account traffic on this connector.
 */
public class RateLimitIlpPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public RateLimitIlpPacketFilter(final Supplier<InterledgerAddress> operatorAddressSupplier) {
    super(operatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    // TODO: Implement this. See https://google.github.io/guava/releases/19.0/api/docs/index.html?com/google/common/util/concurrent/RateLimiter.html

    return filterChain.doFilter(sourceAccountId, sourcePreparePacket);
  }
}
