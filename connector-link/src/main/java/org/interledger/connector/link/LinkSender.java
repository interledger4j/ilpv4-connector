package org.interledger.connector.link;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.InterledgerResponsePacketMapper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Defines how to send data to the other side of a bilateral link (i.e., the other party operating a single account in
 * tandem with the operator of this sender).
 */
@FunctionalInterface
public interface LinkSender {

  /**
   * <p>Sends an ILPv4 request packet to a connected peer and returns the response packet (if one is returned).</p>
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
   *   <ul>An instance of {@link Optional#empty()}, which means the request expired before a response was received.
   *   Note that this type of response does _not_ mean the request wasn't fulfilled or rejected. Instead, it simply
   *   means a response was not received in-time from the remote peer. Because of this, senders should not assume what
   *   actually happened on the org.interledger.bilateral receivers side of this link request.</ul>
   * </ol>
   * </pre>
   *
   * @param preparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  InterledgerResponsePacket sendPacket(InterledgerPreparePacket preparePacket);

}
