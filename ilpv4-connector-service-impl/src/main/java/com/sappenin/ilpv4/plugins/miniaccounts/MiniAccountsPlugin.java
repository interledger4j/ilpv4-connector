package com.sappenin.ilpv4.plugins.miniaccounts;

import com.fasterxml.jackson.databind.JsonNode;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpRequest;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.support.Completions;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An extension of {@link AbstractPlugin} that enables mini-accounts functionality.
 */
public abstract class MiniAccountsPlugin extends AbstractPlugin implements IldcpFetcher {

  /**
   * Ths Address Prefix for this plugin. Determined via {@link MiniAccountsPluginSettings#prefix()} or else via ILDCP.
   */
  private AtomicReference<InterledgerAddressPrefix> pluginPrefix;

  /**
   * Required-args Constructor.
   */
  public MiniAccountsPlugin(final MiniAccountsPluginSettings pluginSettings) {
    super(pluginSettings);
  }

  /**
   * Perform the logic of actually connecting to the remote peer.
   */
  @Override
  public CompletableFuture<Void> doConnect() {


    try {
      return Completions.supplyAsync(() -> {
        // 1. Fetch ILDCP Information.
        IldcpResponse response = this.fetch(IldcpRequest.builder().build());
        // CompletableFuture response = this.sendData(preparePacket);

        // 2. Set a prefix.
        if (pluginPrefix.get() != null) {
          this.pluginPrefix.compareAndSet(null, InterledgerAddressPrefix.from(response.getClientAddress()));
        } else {
          throw new RuntimeException("Plugin prefix was already set!");
        }


        // End...
        return null;
      }).thenApply((val) -> this.doMiniAccountsConnect()).toCompletableFuture().get();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform the logic connecting...
   */
  public abstract CompletableFuture<Void> doMiniAccountsConnect();

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


  /**
   * Send an IL-DCP request to a remote connector, and expect a response containing IL-DCP information to configure this
   * caller.
   *
   * @param ildcpRequest An instance of {@link IldcpRequest} that can be used to make a request to a parent Connector.
   *
   * @return A {@link CompletableFuture} that resolves to the IL-DCP response.
   */
  @Override
  public IldcpResponse fetch(final IldcpRequest ildcpRequest) throws InterledgerProtocolException {
    Objects.requireNonNull(ildcpRequest);
    return null;
    //return this.sendData(IldcpRequestPacket.from(ildcpRequest)).get();
  }
}
