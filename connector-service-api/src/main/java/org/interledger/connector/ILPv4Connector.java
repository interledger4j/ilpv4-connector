package org.interledger.connector;

import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.FxRateOverridesRepository;
import org.interledger.connector.routing.ExternalRoutingService;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settlement.SettlementService;

import com.google.common.eventbus.EventBus;

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
 * @see "https://github.com/interledger/rfcs/blob/master/0001-interledger-architecture"
 */
public interface ILPv4Connector {

  ConnectorSettings getConnectorSettings();

  ILPv4PacketSwitch getIlpPacketSwitch();

  LinkManager getLinkManager();

  AccountManager getAccountManager();

  AccountSettingsRepository getAccountSettingsRepository();

  FxRateOverridesRepository getFxRateOverridesRepository();

  ExternalRoutingService getExternalRoutingService();

  BalanceTracker getBalanceTracker();

  SettlementService getSettlementService();

  /**
   * Accessor for the {@link EventBus} that this Connector uses to propagate internal events of type ConnectorEvent.
   */
  EventBus getEventBus();
}
