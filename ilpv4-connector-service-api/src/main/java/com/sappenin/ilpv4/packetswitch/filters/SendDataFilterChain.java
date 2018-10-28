package com.sappenin.ilpv4.packetswitch.filters;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.util.concurrent.CompletableFuture;

/**
 * A FilterChain is an object provided by the connector to the developer giving a view into the invocation chain of a
 * filtered send-data request. Filters use the FilterChain to invoke the next filter in the chain, or if the calling
 * filter is the last filter in the chain, to invoke the send-data call on the targeted plugin resource at the end of
 * the chain.
 *
 * @see SendDataFilter
 **/
public interface SendDataFilterChain {

  CompletableFuture<InterledgerFulfillPacket> doFilter(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) throws InterledgerProtocolException;

}
