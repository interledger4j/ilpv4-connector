package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A filter is an object that performs filtering tasks on either the request to send money, or on the response from a
 * target plugin, or both.
 *
 * <p>Filters perform filtering in the <code>doFilter</code> method.
 */
public interface SendDataFilter {

  // TODO: Init and Destroy?

  CompletableFuture<Optional<InterledgerResponsePacket>> doFilter(
    InterledgerAddress sourceAccountAddress, InterledgerPreparePacket sourcePreparePacket, SendDataFilterChain filterChain
  );

}
