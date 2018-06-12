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

  private final String interledgerAddress;
  private final AtomicBoolean connected;

  /**
   * Required-args Constructor.
   */
  public MockPlugin(final String interledgerAddress) {
    this.interledgerAddress = Objects.requireNonNull(interledgerAddress);
    this.connected = new AtomicBoolean(false);
  }

  @Override
  public void doConnect() {
    // NO OP
    logger.info("{} connecting to {}...", this.getPluginType().getPluginDescription(), this.getInterledgerAddress());
    this.connected.compareAndSet(false, true);
    logger.info("{} connected to {}!", this.getPluginType().getPluginDescription(), this.getInterledgerAddress());
  }

  @Override
  public void doDisconnect() {
    // NO OP
    logger
      .info("{} disconnecting from {}...", this.getPluginType().getPluginDescription(), this.getInterledgerAddress());
    this.connected.compareAndSet(true, false);
    logger.info("{} disconnected from {}!", this.getPluginType().getPluginDescription(), this.getInterledgerAddress());
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
    logger.info("Settling {} units with {}!", amount, getInterledgerAddress());
  }

  @Override
  public PluginType getPluginType() {
    return PluginType.MOCK;
  }

  @Override
  public String getInterledgerAddress() {
    return this.interledgerAddress;
  }
}