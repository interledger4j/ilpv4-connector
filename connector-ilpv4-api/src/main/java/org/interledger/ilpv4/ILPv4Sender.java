package org.interledger.ilpv4;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>When two parties want to do business online, the one who sends money is the <tt>sender</tt>.</p>
 *
 * <p>Interledger moves money by relaying packets. In the Interledger Protocol, a "prepare" packet represents a
 * possible movement of some money and comes with a condition for releasing it. As the packet moves forward through the
 * chain of connectors, the sender and connectors prepare balance changes for the accounts between them. The connectors
 * also adjust the amount for any currency conversions and fees subtracted.</p>
 *
 * <p>When the prepare packet arrives at the receiver, if the amount of money to be received is acceptable, the
 * receiver fulfills the condition with a "fulfill" packet that confirms the balance change in the account between the
 * last connector and the receiver. Connectors pass the "fulfill" packet back up the chain, confirming the planned
 * balance changes along the way, until the sender's money has been paid to the first connector.</p>
 *
 * <p>At any step along the way, a connector or the receiver can reject the payment, sending a "reject" packet back up
 * the chain. This can happen if the receiver doesn't want the money, or a connector can't forward it. A prepared
 * payment can also expire without the condition being fulfilled. In all these cases, no balances change.</p>
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0001-interledger-architecture/"
 * @see "https://github.com/interledger/rfcs/blob/master/0027-interledger-protocol-4/"
 * @deprecated It's unclear if this interface is useful. Likely the handleData will be defined in the Connector
 * inteface. Otherwise, a plugin should be used if something isn't a connector.
 */
@Deprecated
public interface ILPv4Sender extends ILPv4Node {

  /**
   * <p>Sends an ILPv4 request packet to the connected peer and returns the response packet.</p>
   *
   * <p>This method supports one of three responses, which can be handled by using the
   * {@link InterledgerResponsePacket#handle(Consumer, Consumer)} and {@link InterledgerResponsePacket#map(Function,
   * Function)}.
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
   * @param sourcePreparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  CompletableFuture<Optional<InterledgerResponsePacket>> sendData(InterledgerPreparePacket sourcePreparePacket);

  //  /**
  //   * <p>Sends an ILPv4 request packet to the connected peer and handles the response, if any.</p>
  //   *
  //   * @param preparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
  //   * @param packetHandler A {@link InterledgerResponsePacketHandler} that can handle the async response.
  //   *
  //   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
  //   * represents either
  //   */
  //  default void sendData(final InterledgerPreparePacket preparePacket, InterledgerResponsePacketHandler packetHandler) {
  //    Objects.requireNonNull(preparePacket);
  //    Objects.requireNonNull(packetHandler);
  //    packetHandler.handle(sendData(preparePacket).join());
  //  }

}
