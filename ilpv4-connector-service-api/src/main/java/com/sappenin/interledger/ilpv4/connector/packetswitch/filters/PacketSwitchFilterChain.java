package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;

/**
 * This filter-chain provides the developer a view into the invocation chain of a filtered send-data request. Filters
 * use the this contract to invoke the next filter in the chain, or if the calling filter is the last filter in the
 * chain, to invoke the send-data call on the targeted plugin resource at the end of the chain.
 *
 * @see PacketSwitchFilter
 **/
public interface PacketSwitchFilterChain {

  Optional<InterledgerResponsePacket> doFilter(
    AccountId sourceAccountId, InterledgerPreparePacket sourcePreparePacket
  );

}
