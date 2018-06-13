package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.model.PluginType;
import org.interledger.core.Fulfillment;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link Plugin} that simulates all calls to a faux remote peer.
 */
public class MockPlugin implements Plugin {

  private static final String PREIMAGE = "Roads? Where we're going we don't need roads!";
  private static final String ILP_DATA = "MARTY!";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // The ILP Address for this plugin.
  private final String interledgerAddress;
  // The ILP Address of the Connector operating this plugin.
  private final String connectorInterledgerAddress;
  private final AtomicBoolean connected;

  /**
   * Required-args Constructor.
   *
   * @param interledgerAddress          The Interledger Address for this plugin.
   * @param connectorInterledgerAddress The Interledger Address for the Connector operating this plugin.
   */
  public MockPlugin(final String interledgerAddress, final String connectorInterledgerAddress) {
    this.interledgerAddress = Objects.requireNonNull(interledgerAddress);
    this.connectorInterledgerAddress = Objects.requireNonNull(connectorInterledgerAddress);
    this.connected = new AtomicBoolean(false);
  }

  @Override
  public void doConnect() {
    // NO OP
    logger.info("{} for {} connecting to {}...", this.getPluginType().getPluginDescription(),
      this.getInterledgerAddress(), this.getConnectorInterledgerAddress());
    this.connected.compareAndSet(false, true);
    logger
      .info("{} for {} connected to {}!", this.getPluginType().getPluginDescription(), this.getInterledgerAddress(),
        this.getConnectorInterledgerAddress());
  }

  @Override
  public void doDisconnect() {
    // NO OP
    logger
      .info("{} for {} disconnecting from {}...", this.getPluginType().getPluginDescription(),
        this.getInterledgerAddress(), this.getConnectorInterledgerAddress());
    this.connected.compareAndSet(true, false);
    logger.info("{} for {} disconnected from {}!", this.getPluginType().getPluginDescription(),
      this.getInterledgerAddress(), this.getConnectorInterledgerAddress());
  }

  /**
   * ILP Prepare packets always fulfill in this Mock plugin.
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> sendPacket(InterledgerPreparePacket preparePacket) throws InterledgerProtocolException {
    final InterledgerFulfillPacket ilpFulfillmentPacket = InterledgerFulfillPacket.builder()
      .fulfillment(Fulfillment.of(PREIMAGE.getBytes()))
      .data(ILP_DATA.getBytes())
      .build();

    return CompletableFuture.supplyAsync(() -> ilpFulfillmentPacket);
  }

  @Override
  public void settle(BigInteger amount) {
    // NO OP
    logger.info("{} settling {} units via {}!", this.getConnectorInterledgerAddress(), amount,
      this.getInterledgerAddress());
  }

  @Override
  public PluginType getPluginType() {
    return PluginType.MOCK;
  }

  @Override
  public String getInterledgerAddress() {
    return this.interledgerAddress;
  }

  @Override
  public String getConnectorInterledgerAddress() {
    return this.connectorInterledgerAddress;
  }

}