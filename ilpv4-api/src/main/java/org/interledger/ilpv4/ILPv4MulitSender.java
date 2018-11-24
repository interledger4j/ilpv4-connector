package org.interledger.ilpv4;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.InterledgerResponsePacketMapper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Defines how an Interledger Node can send ILPv4 packets to remote account on the Interledger. This interface
 * supports multiple sender-address, unlike its counterpart, which assumes only a single address (see {@link
 * ILPv4Sender}.</p>
 */
public interface ILPv4MulitSender extends ILPv4Node {

  /**
   * <p>Sends an ILPv4 request packet to the connected peer and returns the response packet.</p>
   *
   * <p>This method supports one of three responses, which can be handled by using utility classes such as  {@link
   * InterledgerResponsePacketMapper} or {@link InterledgerResponsePacketHandler}:
   * </p>
   *
   * <pre>
   * <ol>
   *   <ul>An instance of {@link InterledgerFulfillPacket}, which means the packet was fulfilled by the receiver.</ul>
   *   <ul>An instance of {@link InterledgerRejectPacket}, which means the packet was rejected by one of the nodes in
   *   the payment path.
   *   </ul>
   *   <ul>An instance of {@link Optional#empty()}, which means the packet expired.</ul>
   * </ol>
   * </pre>
   *
   * @param sourceAddress       An {@link InterledgerAddress} for the sender of this packet (note that a node may
   *                            support sending from multiple addresses).
   * @param sourcePreparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
    InterledgerAddress sourceAddress, InterledgerPreparePacket sourcePreparePacket
  );

  /**
   * <p>Sends an ILPv4 request packet to the connected peer and handles the response, if any.</p>
   *
   * @param sourceAddress An {@link InterledgerAddress} for the sender of this packet (note that a node may * support
   *                      sending from multiple addresses).
   * @param preparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * represents either
   */
  default void sendData(
    InterledgerAddress sourceAddress, InterledgerPreparePacket preparePacket,
    InterledgerResponsePacketHandler packetHandler
  ) {
    packetHandler.handle(sendData(sourceAddress, preparePacket).join());
  }

}
