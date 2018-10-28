package com.sappenin.ilpv4;

import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A default implementation of {@link IlpConnector}.
 */
public class DefaultIlpConnector implements IlpConnector {

  // TODO: Use Prefix...
  // Used to determine if a message is for a peer (such as for routing) for for regular data/payment messages.
  public static final String PEER_PROTOCOL_PREFIX = "peer.";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final AccountManager accountManager;
  private final PaymentRouter<Route> paymentRouter;
  private final IlpPacketSwitch ilpPacketSwitch;
  //private final Plugin.IlpDataHandler ilpPluginDataHandler;
  //private final Plugin.IlpMoneyHandler ilpMoneyHandler;

  public DefaultIlpConnector(
    final Supplier<ConnectorSettings> connectorSettingsSupplier, final AccountManager accountManager,
    final PaymentRouter<Route> paymentRouter,
    //    final Plugin.IlpDataHandler ilpPluginDataHandler,
    //    final Plugin.IlpMoneyHandler ilpMoneyHandler,
    final IlpPacketSwitch ilpPacketSwitch
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);

    this.accountManager = Objects.requireNonNull(accountManager);
    this.paymentRouter = Objects.requireNonNull(paymentRouter);
    //this.ilpPluginDataHandler = Objects.requireNonNull(ilpPluginDataHandler);
    //this.ilpMoneyHandler = Objects.requireNonNull(ilpMoneyHandler);
    this.ilpPacketSwitch = Objects.requireNonNull(ilpPacketSwitch);
  }

  @PostConstruct
  private final void init() {
    for (AccountSettings accountSettings : getConnectorSettings().getAccountSettings()) {
      accountManager.add(accountSettings);
    }

    // Add this Connector as the DataHandler for each Plugin...
    //    this.accountManager.getAllAccountSettings()
    //      .map(AccountSettings::getInterledgerAddress)
    //      .map(accountManager::getPlugin)
    //      .filter(Optional::isPresent)
    //      .map(Optional::get)
    //      .forEach(plugin -> plugin.registerDataHandler(this.ilpPluginDataHandler));
  }

  @PreDestroy
  public void shutdown() {
    // accountManager#shutdown is called automatically by spring due to naming convention, so no need to call it here.
  }

  // TODO: Make this a supplier.
  @Override
  public ConnectorSettings getConnectorSettings() {
    return this.connectorSettingsSupplier.get();
  }

  //  @Override
  //  public Plugin.IlpDataHandler getIlpPluginDataHandler() {
  //    return this.ilpPluginDataHandler;
  //  }
  //
  //  @Override
  //  public Plugin.IlpMoneyHandler getIlpPluginMoneyHandler() {
  //    return this.ilpMoneyHandler;
  //  }

  @Override
  public AccountManager getAccountManager() {
    return this.accountManager;
  }

  //  /**
  //   * Find the appropriate lpi2 to send the outbound packet to.
  //   *
  //   * @param nextHopAccount
  //   * @param nextHopPacket
  //   */
  //  @VisibleForTesting
  //  protected CompletableFuture<InterledgerFulfillPacket> outbound(
  //    final InterledgerAddress nextHopAccount, final InterledgerPreparePacket nextHopPacket
  //  ) {
  //    Objects.requireNonNull(nextHopAccount);
  //    Objects.requireNonNull(nextHopPacket);
  //
  //    return this.accountManager.getOrCreatePlugin(nextHopAccount).sendData(nextHopPacket);
  //  }

  @Override
  public PaymentRouter<Route> getPaymentRouter() {
    return this.paymentRouter;
  }

  @Override
  public IlpPacketSwitch getIlpPacketSwitch() {
    return this.ilpPacketSwitch;
  }

}
