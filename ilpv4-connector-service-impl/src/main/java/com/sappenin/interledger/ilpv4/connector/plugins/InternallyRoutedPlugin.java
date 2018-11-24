package com.sappenin.interledger.ilpv4.connector.plugins;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.link.BilateralDataHandler;
import org.interledger.plugin.link.BilateralMoneyHandler;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.exceptions.DataHandlerAlreadyRegisteredException;
import org.interledger.plugin.lpiv2.exceptions.MoneyHandlerAlreadyRegisteredException;
import org.interledger.plugin.lpiv2.settings.PluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract extension of {@link AbstractPlugin} that provides all base-line functionality for an internally-routed
 * plugin.
 *
 * As an example, a connector will have certain special addresses that should not be forwarded, such as `peer.config` .
 * In this case, the connector's routing-table is configured to forward all traffic for such internally-routed
 * destinations to specifically configured plugins. In the case of the peer-config protocol, packets will come into the
 * connector with that destination address, and the routing table will forward to the appropriate internally-routed
 * plugin.
 *
 * In this way, these plugins are like a sub-network for this connector. Traffic is still routed using Interledger, but
 * it does not exit the connector like a normally schemed packet.
 *
 * <pre>
 *                                            ┌────────────┐             ┌────────────────┐
 *                ┌────────┐                  │ Connector  │             │   Internally   │
 * ────sendData──▷│ Plugin │──onIncomingData─▷│   Switch   │───sendData─▶│ Routed Plugin  │
 *                └────────┘                  └────────────┘             └────────────────┘
 *
 * </pre>
 */
public abstract class InternallyRoutedPlugin extends AbstractPlugin<PluginSettings> implements Plugin<PluginSettings> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final CodecContext oerCodecContext;

  /**
   * Required-args constructor.
   *
   * @param pluginSettings
   * @param oerCodecContext
   */
  protected InternallyRoutedPlugin(final PluginSettings pluginSettings, final CodecContext oerCodecContext) {
    super(pluginSettings);
    this.oerCodecContext = Objects.requireNonNull(oerCodecContext);
  }

  @Override
  public CompletableFuture<Void> doConnect() {
    // No-op. Internally-routed plugins are always connected, so there is no connect/disconnect logic required.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> doDisconnect() {
    // No-op. Internally-routed plugins are always connected, so there is no connect/disconnect logic required.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  protected CompletableFuture<Void> doSendMoney(BigInteger amount) {
    // No-op. Internally-routed plugins don't involve settlement.
    return CompletableFuture.completedFuture(null);
  }

  public CodecContext getOerCodecContext() {
    return oerCodecContext;
  }

  @Override
  public void registerDataHandler(BilateralDataHandler ilpDataHandler) throws DataHandlerAlreadyRegisteredException {
    throw new RuntimeException(
      "Internally-routed plugins never have incoming data, and thus should not have a registered DataHandler."
    );
  }

  @Override
  public void registerMoneyHandler(BilateralMoneyHandler moneyHandler) throws MoneyHandlerAlreadyRegisteredException {
    throw new RuntimeException(
      "Internally-routed plugins never settle, and thus should not have a registered MoneyHandler."
    );
  }
}
