package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.model.AccountId;
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
  private final AccountId accountId;
  private final AtomicBoolean connected;

  /**
   * Required-args Constructor.
   */
  public MockPlugin(final AccountId accountId) {
    this.accountId = Objects.requireNonNull(accountId);
    this.connected = new AtomicBoolean(false);
  }

  @Override
  public void doConnect() {
    // NO OP
    logger.info("{} connecting to {}...", this.getPluginType().getPluginDescription(), this.getAccountId());
    this.connected.compareAndSet(false, true);
    logger.info("{} connected to {}!", this.getPluginType().getPluginDescription(), this.getAccountId());
  }

  @Override
  public void doDisconnect() {
    // NO OP
    logger.info("{} disconnecting to {}...", this.getPluginType().getPluginDescription(), this.getAccountId());
    this.connected.compareAndSet(true, false);
    logger.info("{} disconnected to {}!", this.getPluginType().getPluginDescription(), this.getAccountId());
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
    logger.info("Settling {} units!", amount);
  }

  @Override
  public PluginType getPluginType() {
    return PluginType.MOCK;
  }

  @Override
  public AccountId getAccountId() {
    return this.accountId;
  }
}