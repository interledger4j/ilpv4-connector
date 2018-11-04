package com.sappenin.ilpv4.client;

import com.google.common.base.Preconditions;
import org.interledger.core.*;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.btp2.BtpClientPluginSettings;
import org.interledger.plugin.lpiv2.events.PluginEventHandler;
import org.interledger.plugin.lpiv2.exceptions.DataHandlerAlreadyRegisteredException;
import org.interledger.plugin.lpiv2.exceptions.MoneyHandlerAlreadyRegisteredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of {@link IlpClient} that connects to a remote peer using BTP, but merely logs all responses.
 */
public class BtpWsClient implements IlpClient<BtpClientPluginSettings> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Plugin<BtpClientPluginSettings> pluginDelegate;

  /**
   * No-args Constructor.
   */
  public BtpWsClient() {
  }

  /**
   * Required-args Constructor.
   */
  public BtpWsClient(
    final Plugin<BtpClientPluginSettings> pluginDelegate
  ) {
    this.pluginDelegate = Objects.requireNonNull(pluginDelegate);

    this.pluginDelegate.unregisterDataHandler();
    this.pluginDelegate.registerDataHandler(((sourceAccountAddress, sourcePreparePacket) -> {
      final String warningMessage = String.format(
        "ILP BtpWsClient ignores incoming packets by default. Register a different handler to change this behavior. " +
          "sourceAccountAddress=`%s` sourcePreparePacket=%s",
        sourceAccountAddress, sourcePreparePacket
      );
      logger.warn(warningMessage);
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .message("This client does not accept incoming sendData packets!")
          .triggeredBy(pluginDelegate.getPluginSettings().getLocalNodeAddress())
          .build()
      );
    }));

    this.pluginDelegate.unregisterMoneyHandler();
    this.pluginDelegate.registerMoneyHandler(((amount) -> {
      final String warningMessage = String.format(
        "ILP BtpWsClient ignores incoming SendMoney requests by default. " +
          "Register a different handler to change this behavior. amount: `%s`", amount
      );
      logger.warn(warningMessage);
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .message("This client does not accept incoming sendMoney requests!")
          .triggeredBy(pluginDelegate.getPluginSettings().getLocalNodeAddress())
          .build());
    }));
  }

  @Override
  public BtpClientPluginSettings getPluginSettings() {
    assertPluginDelegateExists();
    return pluginDelegate.getPluginSettings();
  }

  @Override
  public CompletableFuture<Void> connect() {
    assertPluginDelegateExists();
    return pluginDelegate.connect();
  }

  @Override
  public CompletableFuture<Void> disconnect() {
    assertPluginDelegateExists();
    return pluginDelegate.disconnect();
  }

  @Override
  public boolean isConnected() {
    assertPluginDelegateExists();
    return pluginDelegate.isConnected();
  }

  @Override
  public CompletableFuture<InterledgerFulfillPacket> sendData(final InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException {
    assertPluginDelegateExists();
    return pluginDelegate.sendData(preparePacket);
  }

  @Override
  public CompletableFuture<Void> sendMoney(BigInteger amount) {
    assertPluginDelegateExists();
    return pluginDelegate.sendMoney(amount);
  }

  @Override
  public UUID addPluginEventHandler(PluginEventHandler eventHandler) {
    assertPluginDelegateExists();
    return pluginDelegate.addPluginEventHandler(eventHandler);
  }

  @Override
  public void removePluginEventHandler(UUID eventHandlerId) {
    assertPluginDelegateExists();
    pluginDelegate.removePluginEventHandler(eventHandlerId);
  }

  @Override
  public void registerDataHandler(IlpDataHandler ilpDataHandler) throws DataHandlerAlreadyRegisteredException {
    assertPluginDelegateExists();
    pluginDelegate.registerDataHandler(ilpDataHandler);
  }

  @Override
  public IlpDataHandler getDataHandler() {
    assertPluginDelegateExists();
    return pluginDelegate.getDataHandler();
  }

  @Override
  public void unregisterDataHandler() {
    assertPluginDelegateExists();
    pluginDelegate.unregisterDataHandler();
  }

  @Override
  public void registerMoneyHandler(IlpMoneyHandler ilpMoneyHandler) throws MoneyHandlerAlreadyRegisteredException {
    assertPluginDelegateExists();
    pluginDelegate.registerMoneyHandler(ilpMoneyHandler);
  }

  @Override
  public void unregisterMoneyHandler() {
    assertPluginDelegateExists();
    pluginDelegate.unregisterMoneyHandler();
  }

  @Override
  public IlpMoneyHandler getMoneyHandler() {
    assertPluginDelegateExists();
    return pluginDelegate.getMoneyHandler();
  }

  private void assertPluginDelegateExists() {
    Preconditions.checkArgument(
      pluginDelegate != null,
      "PluginDelegate must be initialized before using this client!"
    );
  }

  @Override
  public Plugin<BtpClientPluginSettings> getPluginDelegate() {
    return this.pluginDelegate;
  }

  @Override
  public void setPluginDelegate(final Plugin<BtpClientPluginSettings> pluginDelegate) {
    this.pluginDelegate = Objects.requireNonNull(pluginDelegate);
  }
}