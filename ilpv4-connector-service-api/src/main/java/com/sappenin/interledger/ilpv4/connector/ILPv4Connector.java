package com.sappenin.interledger.ilpv4.connector;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingService;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.InterledgerResponsePacketMapper;
import org.interledger.ilpv4.ILPv4Node;
import org.interledger.ilpv4.ILPv4Receiver;
import org.interledger.ilpv4.ILPv4Sender;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>When two parties want to do business online, the one who sends money is the sender and the one who gets money is
 * the receiver. If the sender and the receiver don't have some monetary system in common, they need one or more parties
 * to connect them. In the Interledger architecture, connectors forward money through the network until it reaches the
 * receiver.</p>
 *
 * <p>An Interledger Connector is an extension of {@link ILPv4Node} that operates as an ILP packet-switch, accepting
 * incoming ILPv4 packets from an incoming link, and then routing those packets to an appropriate outgoing link, and
 * then processing a response back through the original link connection.</p>
 *
 * <p>This interface extends both {@link ILPv4Sender} and {@link ILPv4Receiver}, and also has a
 * {@link ILPv4PacketSwitch}.</p>
 *
 * <p>Connectors provide a service of forwarding packets and relaying money, and they take on some risk when they do
 * so. In exchange, connectors can charge fees and derive a profit from these services. In the open network of the
 * Interledger, connectors are expected to compete among one another to offer the best balance of speed, reliability,
 * coverage, and cost.</p>
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0001-interledger-architecture"
 */
public interface ILPv4Connector extends ILPv4Node {

  ConnectorSettings getConnectorSettings();

  // TODO: Consider making the Connector just BE the packet switch?
  ILPv4PacketSwitch getIlpPacketSwitch();


  /**
   * <p>Sends an ILPv4 request packet to the appropriate place. Implementation of this method should generally
   * forward this call to the packet-switch, but may introduce custom routing logic, such as for sending packets that
   * should not be packet-switched.</p>
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
   * @param sourcePreparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
    AccountId sourceAccountId, InterledgerPreparePacket sourcePreparePacket
  );

  /**
   * Handles an incoming {@link InterledgerPreparePacket} received from a connected peer. Implementation of this method
   * should generally forward this call to the packet-switch, but may introduce custom routing logic, such as for
   * handling packets that should not be packet-switched.
   *
   * @param accountId             An {@link AccountId} for the remote peer (directly connected to this Connector) that
   *                              immediately sent the packet.
   * @param incomingPreparePacket A {@link InterledgerPreparePacket} containing data about an incoming payment.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingData(
    AccountId accountId, InterledgerPreparePacket incomingPreparePacket
  );

  //TODO: Consider how the notion of an Account fits into the ILP Connector stack. Accounts exist between two
  // Interledger nodes, so since a Connector is a node, it makes sense for it to hold accounts. However, there is a
  // case that all nodes should hold at least one account, possibly more, so perhaps this method belongs at the node
  // level.
  AccountManager getAccountManager();

  RoutingService getRoutingService();
}
