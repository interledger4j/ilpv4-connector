package com.sappenin.ilpv4.plugins;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.math.BigInteger;
import java.util.concurrent.Future;

/**
 * <p>An abstraction for communicating with a remote peer.</p>
 *
 * The overall flow of funds in ILPv4 is as follows:
 *
 * <pre>
 * Sender --sendPacket-> Connector 1 --sendPacket-> Connector 2 --sendPacket-> Receiver
 *    |                        |                        |
 *    `----sendMoney->         `----sendMoney->         `----sendMoney->
 * </pre>
 *
 * Sender/Connector's call <tt>sendData</tt>, wait for a fulfillment, and then call
 * <tt>sendMoney</tt> if the fulfillment is valid.
 */
public interface Plugin {

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
  Future<InterledgerFulfillPacket> sendPacket(InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException;

  /**
   * Transfer amount units of money from the caller to the counterparty of the account.
   *
   * @param amount
   */
  void sendMoney(BigInteger amount);
}
