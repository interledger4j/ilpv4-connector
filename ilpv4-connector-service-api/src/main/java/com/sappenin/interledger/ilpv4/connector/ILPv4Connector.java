package com.sappenin.interledger.ilpv4.connector;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.InternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.ilpv4.ILPv4Node;
import org.interledger.ilpv4.ILPv4Receiver;
import org.interledger.ilpv4.ILPv4Sender;

/**
 * <p>When two parties want to exchange value online, the one who sends money is the sender and the one who gets
 * money is the receiver. If the sender and the receiver don't have some monetary system in common, they need one or
 * more parties to connect them. In the Interledger architecture, connectors forward money through the network until it
 * reaches the receiver.</p>
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

  ILPv4PacketSwitch getIlpPacketSwitch();

  //TODO: Consider how the notion of an Account fits into the ILP Connector stack. Accounts exist between two
  // Interledger nodes, so since a Connector is a node, it makes sense for it to hold accounts. However, there is a
  // case that all nodes should hold at least one account, possibly more, so perhaps this method belongs at the node
  // level.
  AccountManager getAccountManager();

  LinkManager getLinkManager();

  ExternalRoutingService getExternalRoutingService();

  InternalRoutingService getInternalRoutingService();
}
