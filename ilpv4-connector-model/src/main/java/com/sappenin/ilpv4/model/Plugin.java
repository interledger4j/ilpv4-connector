package com.sappenin.ilpv4.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * <p>An abstraction for communicating with a remote Interledger peer.</p>
 *
 * <p>The overall flow of funds in ILPv4 is as follows:
 *
 * <pre>
 * Sender --sendPacket-> Connector 1 --sendPacket-> Connector 2 --sendPacket-> Receiver
 *    |                        |                        |
 *    `----settle->            `----settle->            `----settle->
 * </pre>
 *
 * Sender/Connector's call <tt>sendData</tt>, wait for a fulfillment, and then call
 * <tt>settle</tt> (possibly infrequently or even only eventually for bulk settlement) if the fulfillment is valid.</p>
 */
public interface Plugin {

  /**
   * <p>The Interledger address of the remote account.</p>
   *
   * <p>While a single node might operate upon multiple account addresses, a given plugin connection will always be
   * initiated to only a single peer at a time (for a Connector to connect to multiple peers, the Connector must
   * instantiate multiple plugins).</p>
   */
  InterledgerAddress getAccountAddress();

  /**
   * Accessor for the type of this plugin.
   *
   * @return An instance of {@link PluginType}.
   */
  PluginType getPluginType();

  void doConnect();

  /**
   * Determines if a plugin is connected or not.
   *
   * @return {@code true} if the plugin is connected; {@code false} otherwise.
   */
  boolean isConnected();

  void doDisconnect();

  /**
   * Sends an ILP request packet to the peer and returns the response packet (this method correlates with
   * <tt>sendData</tt> in the Javascript connector).
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
   * Handle an incoming Interledger data packets. If an error occurs, this method MAY throw an exception. In general,
   * the callback should behave as sendData does.
   *
   * @param preparePacket
   */
  CompletableFuture<InterledgerFulfillPacket> onIncomingPacket(InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException;

  /**
   * Settle an outstanding ILP balance with a counterparty by transferring {@code amount} units of value from this ILP
   * node to the counterparty of the account used by this plugin (this method correlates to <tt>sendMoney</tt> in the
   * Javascript Connector).
   *
   * @param amount The amount of "money" to transfer.
   */
  void settle(BigInteger amount);

  /**
   * Handle a request to settle an outstanding balance.
   *
   * @param amount The amount of "money" to transfer.
   */
  void onIncomingSettle(BigInteger amount);
}
