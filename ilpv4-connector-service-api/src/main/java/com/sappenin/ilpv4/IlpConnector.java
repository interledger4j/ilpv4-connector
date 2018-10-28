package com.sappenin.ilpv4;

import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.plugin.lpiv2.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface IlpConnector {

  ConnectorSettings getConnectorSettings();

 // Plugin.IlpDataHandler getIlpPluginDataHandler();

 // Plugin.IlpMoneyHandler getIlpPluginMoneyHandler();

  AccountManager getAccountManager();

  /**
   * Accessor for the {@link PaymentRouter} used by this account manager.
   *
   * @return
   */
  PaymentRouter<Route> getPaymentRouter();

  IlpPacketSwitch getIlpPacketSwitch();

}
