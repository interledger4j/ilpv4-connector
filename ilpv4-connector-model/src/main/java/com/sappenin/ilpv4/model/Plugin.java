package com.sappenin.ilpv4.model;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * <p>An abstraction for communicating with a remote peer.</p>
 *
 * The overall flow of funds in ILPv4 is as follows:
 *
 * <pre>
 * Sender --sendPacket-> Connector 1 --sendPacket-> Connector 2 --sendPacket-> Receiver
 *    |                        |                        |
 *    `----settle->            `----settle->            `----settle->
 * </pre>
 *
 * Sender/Connector's call <tt>sendData</tt>, wait for a fulfillment, and then call
 * <tt>settle</tt> if the fulfillment is valid.
 */
public interface Plugin {

  /**
   * Returns the account identifier that this plugin instance is using.
   *
   * @return An instance of {@link AccountId}.
   */
  String getInterledgerAddress();

  void doConnect();

  void doDisconnect();

  /**
   * Sends an ILP request packet to the peer and returns the response packet.
   *
   * @param preparePacket The ILP packet to send to the peer.
   *
   * @return A {@link Future} that resolves to the ILP response from the peer.
   *
   * @throws InterledgerProtocolException if the request is rejected by the peer.
   */
  CompletableFuture<InterledgerFulfillPacket> sendPacket(InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException;

  /**
   * Settle an outstanding ILP balance with a counterparty by transferring {@code amount} units of value from this ILP
   * node to the counterparty of the account used by this plugin.
   *
   * @param amount The amount of "money" to transfer.
   */
  void settle(BigInteger amount);

  /**
   * Accessor for the type of this plugin.
   *
   * @return An instance of {@link PluginType}.
   */
  PluginType getPluginType();
}
