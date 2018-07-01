package com.sappenin.ilpv4.plugins;

import org.interledger.core.InterledgerAddress;
import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.model.PluginType;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


// TODO make this implement all BTP functionality, and make setttle/sendMoney a no-op so that this can be used to
// represent a data channel with no money involved.

/**
 * An implementation of {@link Plugin} that simulates a relationship with a parent node where this connector is the
 * child.
 */
public abstract class AbstractBtpPlugin implements Plugin {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // The ILP Address for this plugin.
  private final InterledgerAddress interledgerAddress;
  // The ILP Address of the Connector operating this plugin.
  private final ConnectorSettings connectorSettings;
  private final AtomicBoolean connected;

  /**
   * Required-args Constructor.
   *
   * @param connectorSettings
   * @param interledgerAddress The Interledger Address for this plugin.
   */
  public AbstractBtpPlugin(final ConnectorSettings connectorSettings, final InterledgerAddress interledgerAddress) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.interledgerAddress = Objects.requireNonNull(interledgerAddress);
    this.connected = new AtomicBoolean(false);
  }

  @Override
  public void doConnect() {
    // NO OP
    logger.info("[{}] {} connecting to {}...", this.getPluginType().getPluginDescription(),
      this.getPeerAddress(), connectorSettings.getIlpAddress());
    this.connected.compareAndSet(false, true);
    logger
      .info("[{}] {} connected to {}!", this.getPluginType().getPluginDescription(), this.getPeerAddress(),
        connectorSettings.getIlpAddress());
  }

  @Override
  public void doDisconnect() {
    // NO OP
    logger
      .info("[{}] for {} disconnecting from {}...", this.getPluginType().getPluginDescription(),
        this.getPeerAddress(), connectorSettings.getIlpAddress());
    this.connected.compareAndSet(true, false);
    logger.info("[{}] for {} disconnected from {}!", this.getPluginType().getPluginDescription(),
      this.getPeerAddress(), connectorSettings.getIlpAddress());
  }

  /**
   * ILP Prepare packets always fulfill in this Mock plugin.
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> sendPacket(InterledgerPreparePacket preparePacket) throws InterledgerProtocolException {
    //    final InterledgerFulfillPacket ilpFulfillmentPacket = InterledgerFulfillPacket.builder()
    //      .fulfillment(Fulfillment.of(PREIMAGE.getBytes()))
    //      .data(ILP_DATA.getBytes())
    //      .build();
    //
    //    return CompletableFuture.supplyAsync(() -> ilpFulfillmentPacket);
    return null;
  }

  /**
   * Handle an incoming Interledger data packets. If an error occurs, this method MAY throw an exception. In general,
   * the callback should behave as sendData does.
   *
   * @param preparePacket
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> onIncomingPacket(InterledgerPreparePacket preparePacket) throws InterledgerProtocolException {
    return null;
  }


  @Override
  public void settle(BigInteger amount) {
    // NO OP
    logger.info("[{}] settling {} units via {}!",
      connectorSettings.getIlpAddress(), amount, this.getPeerAddress()
    );
  }

  @Override
  public void onIncomingSettle(final BigInteger amount) {
    Objects.requireNonNull(amount);

  }

  @Override
  public PluginType getPluginType() {
    return PluginType.MOCK;
  }

  @Override
  public InterledgerAddress getPeerAddress() {
    return this.interledgerAddress;
  }
}