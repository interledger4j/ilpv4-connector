package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.SendDataFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.SendDataFilterChain;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.InterledgerResponsePacketMapper;
import org.interledger.plugin.lpiv2.Plugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A switching fabric for mapping incoming ILP packets to outgoing links.</p>
 *
 * <p>A packet switch allows for one or more PacketHandlers that are applied to every Packet that traverses this
 * switch's routing fabric.</p>
 *
 * <p>In the happy path, a packet will come into the connector from a particular {@link Plugin} and then will
 * be handed off to this switch to be routed to an appropriate {@link Plugin}. This will result in either an {@link
 * InterledgerFulfillPacket} or an {@link InterledgerRejectPacket}, which can then be returned back to the original
 * caller via the plugin that originated the {@link InterledgerPreparePacket}.</p>
 */
public interface ILPv4PacketSwitch {

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
   * @param sourceAccountId     An {@link AccountId} that identifies the source account that this packet came from.
   * @param sourcePreparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
    AccountId sourceAccountId, InterledgerPreparePacket sourcePreparePacket
  );

  // NOTE: The ILP PacketSwitch layer doesn't expressly have a "sendMoney" handler because this action is a bilateral
  // concern of an account/plugin, and not a Interledger-layer concern. It likewise does not have an onDataHandler
  // because this switch always only sends data, and returns a response.

  boolean add(SendDataFilter sendDataFilter);

  void addFirst(SendDataFilter sendDataFilter);

  SendDataFilterChain getSendDataFilterChain();

  PaymentRouter<Route> getPaymentRouter();
}
