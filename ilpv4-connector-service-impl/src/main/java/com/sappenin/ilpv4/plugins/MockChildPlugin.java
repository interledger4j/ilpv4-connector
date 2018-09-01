package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.model.PluginType;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import org.interledger.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link Plugin} that simulates a relationship with a parent node where this connector is the
 * child.
 */
public class MockChildPlugin implements Plugin {

  private static final String PREIMAGE = "Roads? Where we're going we don't need roads!";
  private static final String ILP_DATA = "MARTY!";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // The ILP Address for this plugin.
  private final InterledgerAddress accountAddress;

  // The ILP Address of the Connector operating this plugin.
  private final ConnectorSettings connectorSettings;
  private final AtomicBoolean connected;

  /**
   * Required-args Constructor.
   *
   * @param connectorSettings Settings for the overall connector.
   * @param accountAddress    The {@link InterledgerAddress} that this plugin is connected to.
   */
  public MockChildPlugin(final ConnectorSettings connectorSettings, final InterledgerAddress accountAddress) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.accountAddress = Objects.requireNonNull(accountAddress);
    this.connected = new AtomicBoolean(false);
  }

  @Override
  public void doConnect() {
    if (!this.isConnected()) {
      // NO OP
      logger.info("[{}] `{}` CONNECTING to `{}`!", this.getPluginType().getPluginDescription(),
        connectorSettings.getIlpAddress().getValue(), this.getAccountAddress().getValue()
      );
      this.connected.compareAndSet(false, true);
      logger
        .info("[{}] `{}` CONNECTED to `{}`!", this.getPluginType().getPluginDescription(),
          connectorSettings.getIlpAddress().getValue(), this.getAccountAddress().getValue()
        );
    }
  }

  @Override
  public boolean isConnected() {
    return this.connected.get();
  }

  @Override
  public void doDisconnect() {
    // NO OP
    logger
      .info("[{}] `{}` disconnecting from `{}`...", this.getPluginType().getPluginDescription(),
        connectorSettings.getIlpAddress().getValue(), this.getAccountAddress().getValue()
      );
    this.connected.compareAndSet(true, false);
    logger.info("[{}] `{}` disconnected from `{}`!", this.getPluginType().getPluginDescription(),
      connectorSettings.getIlpAddress().getValue(), this.getAccountAddress().getValue()
    );
  }

  /**
   * ILP Prepare packets always fulfill in this Mock plugin.
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> sendPacket(InterledgerPreparePacket preparePacket) throws InterledgerProtocolException {
    final InterledgerFulfillPacket ilpFulfillmentPacket = InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(PREIMAGE.getBytes()))
      .data(ILP_DATA.getBytes())
      .build();

    return CompletableFuture.supplyAsync(() -> ilpFulfillmentPacket);
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
      connectorSettings.getIlpAddress(), amount, this.getAccountAddress()
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

  public InterledgerAddress getAccountAddress() {
    return accountAddress;
  }

}