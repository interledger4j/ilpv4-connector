package com.sappenin.interledger.ilpv4.connector;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.InterledgerResponsePacketMapper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Defines how to send data from a particular Connector account into a Connector.
 */
@FunctionalInterface
public
interface ConnectorAdminSender {

  /**
   * <p>Allows a Connector administrator to send an {@link InterledgerPreparePacket} from a particular account to a
   * connected peer, and then return the response packet (if one is returned).</p>
   *
   * <p>This method supports one of three responses, which can be handled by using utility classes such as {@link
   * InterledgerResponsePacketMapper} or {@link InterledgerResponsePacketHandler}:
   * </p>
   *
   * <pre>
   * <ol>
   *   <ul>An instance of {@link InterledgerFulfillPacket}, which means the packet was fulfilled by the receiver.</ul>
   *   <ul>An instance of {@link InterledgerRejectPacket}, which means the packet was rejected by one of the nodes in
   *   the payment path.
   *   </ul>
   *
   *   // TODO: In this model, everything always completes successfully (even in an expiry case with this empty
   *   //   *   response). This means the actual caller of this method shouldn't be able to control the timeout, and thus this
   *   //   *   method should probably not return a CF. Instead, if a caller wants to wrap this call, they can, but perhaps
   *   //   *   this method should just return the actual value. That said, this is really a Connector-specific function,
   *   //   *   since it needs to function more like a proxy. After everything is built, consider how the _actual_ caller uses
   *   //   *   this method wrt to expiries.
   *
   *   <ul>An instance of {@link Optional#empty()}, which means the request expired before a response was received.
   *   Note that this type of response does _not_ mean the request wasn't fulfilled or rejected. Instead, it simply
   *   means a response was not received in-time from the remote peer. Because of this, senders should not assume what
   *   actually happened on the org.interledger.bilateral receivers side of this link request.</ul>
   * </ol>
   * </pre>
   *
   * @param accountId     The {@link AccountId} to send this packet from.
   * @param preparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
    AccountId accountId, InterledgerPreparePacket preparePacket
  );

}
