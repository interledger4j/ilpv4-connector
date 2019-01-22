package com.sappenin.interledger.ilpv4.connector.plugins;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.DataHandler;
import org.interledger.plugin.MoneyHandler;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.exceptions.DataHandlerAlreadyRegisteredException;
import org.interledger.plugin.lpiv2.exceptions.MoneyHandlerAlreadyRegisteredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * <p>An extension of {@link AbstractPlugin} that provides base-line functionality for all incoming packets destined
 * for `peer.` addresses, which are always handled internally (i.e., handled by this Connector) and never forwarded to a
 * remote peer Connector.</p>
 *
 * <p>Examples of special addresses that should not be forwarded to a peer include <tt>`peer.config`</tt> and
 * any address starting with the <tt>self</tt> prefix.</p>
 *
 * <p>In the case of <tt>`peer.config`</tt>, the connector's routing-table is configured to forward all traffic for
 * such internally-routed destinations to specifically configured plugin that extends this base class. In the case of
 * the peer-config protocol, packets will come into the connector with that destination address, and the routing table
 * will forward to the appropriate plugin that can handle `peer.config` messages per the CCP routing protocol.</p>
 *
 * <p>In this way, plugins that extend this class can function like an sub-network internal to this connector.
 * Traffic is still routed using Interledger, but it does not exit the connector like a normally schemed packet would
 * .</p>
 *
 * <pre>
 *                                            ┌────────────┐             ┌────────────────┐
 *                ┌────────┐                  │ Connector  │             │   Internally   │
 * ───-sendData──▷│ Plugin │──onIncomingData─▷│   Switch   │──routeData─▶│ Routed Plugin  │
 *                └────────┘                  └────────────┘             └────────────────┘
 * </pre>
 */
public abstract class InternallyRoutedPlugin extends AbstractPlugin<PluginSettings> implements Plugin<PluginSettings> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final CodecContext ilpCodecContext;

  /**
   * Required-args constructor.
   *
   * @param pluginSettings
   * @param ilpCodecContext
   */
  protected InternallyRoutedPlugin(final PluginSettings pluginSettings, final CodecContext ilpCodecContext) {
    super(pluginSettings);
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
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
  public CompletableFuture<Void> sendMoney(BigInteger amount) {
    // By default, this is a no-op.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void registerDataHandler(DataHandler ilpDataHandler) throws DataHandlerAlreadyRegisteredException {
    throw new RuntimeException(
      "Internally-routed plugins never have incoming data, and thus should not have a registered DataHandler."
    );
  }

  @Override
  public void registerMoneyHandler(MoneyHandler moneyHandler) throws MoneyHandlerAlreadyRegisteredException {
    throw new RuntimeException(
      "Internally-routed plugins never settle, and thus should not have a registered MoneyHandler."
    );
  }

  /**
   * Accessor for the {@link CodecContext} of this plugin.
   */
  protected CodecContext getIlpCodecContext() {
    return ilpCodecContext;
  }
}
