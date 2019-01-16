package org.interledger.ilpv4;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>When two parties want to do business online, the one who the one who gets money is the <tt>receiver</tt>.</p>
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
 *
 * @deprecated It's unclear if this interface is useful. Likely the handleData will be defined in the Connector
 * inteface. Otherwise, a plugin should be used if something isn't a connector.
 */
@Deprecated
public interface ILPv4Receiver extends ILPv4Node {

//  /**
//   * Handles an incoming {@link InterledgerPreparePacket} received from a connected peer, but that may have originated
//   * from any Interledger sender in the network.
//   *
//   * @param sourceAccountAddress  An {@link InterledgerAddress} for the remote peer (directly connected to this
//   *                              receiver) that immediately sent the packet.
//   * @param incomingPreparePacket A {@link InterledgerPreparePacket} containing data about an incoming payment.
//   *
//   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
//   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
//   */
//  CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingData(
//    AccountId accountId, InterledgerPreparePacket incomingPreparePacket
//  );

}
