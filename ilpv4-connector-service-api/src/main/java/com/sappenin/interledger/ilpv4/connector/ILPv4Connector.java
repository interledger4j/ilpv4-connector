package com.sappenin.interledger.ilpv4.connector;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.link.Link;
import org.interledger.ilpv4.ILPv4Node;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;

/**
 * <p>When two parties want to exchange value online, the one who sends money is the sender and the one who
 * gets money is the receiver. If the sender and the receiver don't have some monetary system in common, they need one
 * or more parties to connect them. In the Interledger architecture, Connectors fill this role by forwarding money
 * through the network until it reaches the receiver.</p>
 *
 * <p>Connectors provide the service of forwarding packets and relaying money, and they take on some risk when they do
 * so. In exchange, connectors can charge fees and derive a profit from these services. In the open network of the
 * Interledger, connectors are expected to compete among one another to offer the best balance of speed, reliability,
 * coverage, and cost.</p>
 *
 * <p>This interface extends {@link ILPv4Node} and functions as an Interledger packet-switch, accepting ILPv4
 * packets from an incoming {@link Link}, routing those packets to an appropriate outgoing {@link Link}, and then
 * processing a response back through the original incoming {@link Link}.</p>
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0001-interledger-architecture"
 */
public interface ILPv4Connector extends ILPv4Node {

  ConnectorSettings getConnectorSettings();

  /**
   * @deprecated This method appears to be unused. Consider removing it.
   */
  @Deprecated
  ILPv4PacketSwitch getIlpPacketSwitch();

  LinkManager getLinkManager();

  AccountManager getAccountManager();

  AccountSettingsRepository getAccountSettingsRepository();

  ExternalRoutingService getExternalRoutingService();

  BalanceTracker getBalanceTracker();
}
