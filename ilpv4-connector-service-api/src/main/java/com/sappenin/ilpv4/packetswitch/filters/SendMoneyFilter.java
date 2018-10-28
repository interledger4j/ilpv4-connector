package com.sappenin.ilpv4.packetswitch.filters;

import org.interledger.core.InterledgerProtocolException;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * A filter is an object that performs filtering tasks on either the request to send money, or on the response from a
 * target plugin, or both.
 *
 * <p>Filters perform filtering in the <code>doFilter</code> method.
 */
public interface SendMoneyFilter {

  // TODO: Init and Destroy.

  CompletableFuture<Void> doFilter(final BigInteger amount) throws InterledgerProtocolException;
}
