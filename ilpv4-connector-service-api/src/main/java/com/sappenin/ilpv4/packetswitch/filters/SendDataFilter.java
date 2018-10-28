package com.sappenin.ilpv4.packetswitch.filters;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.util.concurrent.CompletableFuture;

/**
 * A filter is an object that performs filtering tasks on either the request to send money, or on the response from a
 * target plugin, or both.
 *
 * <p>Filters perform filtering in the <code>doFilter</code> method.
 */
public interface SendDataFilter {

  // TODO: Init and Destroy.

  CompletableFuture<InterledgerFulfillPacket> doFilter(
    InterledgerAddress sourceAccountAddress, InterledgerPreparePacket sourcePreparePacket, SendDataFilterChain filterChain
  ) throws InterledgerProtocolException;


  /**
   * Allows the packetswitch to adjust or react to a particular fulfillment response before sending the response back to the
   * orginal ILP sendData caller.
   *
   * @param fulfillmentPacketResponse
   *
   * @return
   */
  //  InterledgerFulfillPacket processOutgoingData(InterledgerFulfillPacket fulfillmentPacketResponse);

  //void processOutgoingMoney();


}
