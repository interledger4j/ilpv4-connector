package com.sappenin.ilpv4.plugins.virtual;

import com.fasterxml.jackson.databind.JsonNode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.events.PluginEventEmitter;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

// TODO: Finish Me?
public class VirtualPlugin extends AbstractPlugin {

  public VirtualPlugin(PluginSettings pluginSettings) {
    super(pluginSettings);
  }


  /**
   * Perform the logic of actually connecting to the remote peer.
   */
  @Override
  public CompletableFuture<Void> doConnect() {
    return null;
  }

  /**
   * Perform the logic of disconnecting from the remote peer.
   */
  @Override
  public CompletableFuture<Void> doDisconnect() {
    return null;
  }

  /**
   * Perform the logic of sending a packet to a remote peer.
   *
   * @param preparePacket
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> doSendData(InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException {
    return null;
  }

  /**
   * Perform the logic of settling with a remote peer.
   *
   * @param amount
   */
  @Override
  protected CompletableFuture<Void> doSendMoney(BigInteger amount) {
    return null;
  }

  /**
   * In order to use plugin virtual, you need an HTTP endpoint that passes calls to the plugin. The url must take one
   * query parameter (method), as well as a JSON body. The method parameter and parsed body must be passed to the plugin
   * like so:
   *
   * @param method A query-param passed to the RPC endpoint to indicate what this plugin should do with the payload.
   * @param body   A JSON payload sent into this plugin via an HTTP RPC endpoint.
   */
  public void receive(String method, JsonNode body) {

  }


}
