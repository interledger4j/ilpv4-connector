package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilterChain;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.InterledgerResponsePacketMapper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A switching fabric for mapping incoming ILP packets to outgoing links.</p>
 *
 * <p>A packet switch allows for one or more PacketHandlers to applied to every Packet that traverses this
 * switch's routing fabric.</p>
 *
 * <p>In the happy path, a packet will come into the connector from a particular {@link Link} and then will
 * be handed off to this switch to be routed to a (typically) different target {@link Link}. This call will result in
 * either an {@link InterledgerFulfillPacket} or an {@link InterledgerRejectPacket}, which can then be returned back to
 * the original caller via the link that originated the {@link InterledgerPreparePacket}.</p>
 */
public interface ILPv4PacketSwitch {

  /**
   * <p>Routes an incoming ILPv4 request packet to a connected peer and returns the response packet (if one is
   * returned).</p>
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
   * @param accountId             The {@link AccountId} to send this packet from.
   * @param incomingPreparePacket An incoming {@link InterledgerPreparePacket} that should be routed to the most
   *                              appropriate peer connected to this Connector.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  InterledgerResponsePacket routeData(
    AccountId accountId, InterledgerPreparePacket incomingPreparePacket
  );

  // NOTE: The ILP PacketSwitch layer doesn't expressly have a "sendMoney" handler because this action is a bilateral
  // concern of an account/link, and not a Interledger-layer concern. It likewise does not have an onDataHandler
  // because this switch always only sends data, and returns a response.

  boolean add(PacketSwitchFilter packetSwitchFilter);

  void addFirst(PacketSwitchFilter packetSwitchFilter);

  PacketSwitchFilterChain getSendDataFilterChain();

  PaymentRouter<Route> getInternalPaymentRouter();

  PaymentRouter<Route> getExternalPaymentRouter();
}
