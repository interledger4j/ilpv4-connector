package com.sappenin.ilpv4.plugins.btp.ws;

import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.plugins.btp.BtpClientPluginSettings;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BinaryMessageToBtpPacketConverter;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BtpPacketToBinaryMessageConverter;
import com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpPacket;
import org.interledger.btp.BtpResponse;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.PluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * An extension of {@link AbstractWebsocketBtpPlugin} for when the plugin is operating as a Websocket client, making
 * connections to a remote BTP Plugin.
 */
public class ClientWebsocketBtpPlugin extends AbstractWebsocketBtpPlugin<BtpClientPluginSettings> {

  public static final String PLUGIN_TYPE_STRING = "BTP_CLIENT";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientWebsocketBtpPlugin.class);
  // TODO: Implement Reconnector?
  private final StandardWebSocketClient wsClient;

  /**
   * Required-args Constructor.
   */
  public ClientWebsocketBtpPlugin(
    final BtpClientPluginSettings pluginSettings,
    final CodecContext ilpCodecContext,
    final CodecContext btpCodecContext,
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    //final BinaryMessageToBtpErrorConverter binaryMessageToBtpErrorConverter,
    //final IlpLogUtils ilpLogUtils,
    final StandardWebSocketClient wsClient
  ) {
    super(
      pluginSettings, ilpCodecContext, btpCodecContext, btpSubProtocolHandlerRegistry,
      binaryMessageToBtpPacketConverter, btpPacketToBinaryMessageConverter
    );
    this.wsClient = Objects.requireNonNull(wsClient);
  }

  /**
   * Send data using the BTP connection over the {@link #wsClient}. This method is typically called by the plugin in
   * response to {@link #sendData(InterledgerPreparePacket)}.
   *
   * @param btpPacket
   *
   * @return
   */
  @Override
  protected CompletableFuture<BtpResponse> doSendDataOverBtp(final BtpPacket btpPacket) {
    Objects.requireNonNull(btpPacket);

    // Send the BtpMessage to the remote peer using the websocket client....but first translate the BtpMessage to a
    // binary message.
    try {
      final BinaryMessage binaryMessage = this.btpPacketToBinaryMessageConverter.convert(btpPacket);

      // Send the message out. A response will come back as a pending request (see below).
      sendMessage(binaryMessage);

      //transferQueue.poll(5, TimeUnit.MINUTES);

      //Use https://github.com/sarveswaran-m/blockingMap4j
      //      boolean response =
      //        this.transferQueue.removeIf(btpResponse -> btpResponse.getRequestId() == btpMessage.getRequestId());

      return registerPendingResponse(btpPacket.getRequestId());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public BtpPluginType getBtpPluginType() {
    return BtpPluginType.WS_CLIENT;
  }

  /**
   * This method will be called for all plugins. A remote connection needs to be made using a Websocket client.
   */
  @Override
  public CompletableFuture<Void> doConnect() {
    final long requestId = this.nextRequestId();

    try {
      // Connect and initialize the Session...
      this.webSocketSession = Optional.ofNullable(
        wsClient.doHandshake(
          new BinaryWebSocketHandler() {
            @Override
            protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
              // TODO: What does the other side of the Websocket see if there's an exception here?
              onIncomingBinaryMessage(session, message)
                .ifPresent(response -> {
                  try {
                    session.sendMessage(response);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                });
            }
          }, "{scheme}://{localhost}:{port}/btp",
          getPluginSettings().getRemotePeerScheme(),
          getPluginSettings().getRemotePeerHostname(),
          getPluginSettings().getRemotePeerPort()).get()
      );

      // AUTH
      final String authToken = getPluginSettings().getSecret();
      final BtpMessage btpAuthMessage = this.constructAuthMessage(requestId, authToken);
      final BinaryMessage binaryAuthMessage = btpPacketToBinaryMessageConverter.convert(btpAuthMessage);
      LOGGER.debug(
        "Websocket Auth BinaryMessage Bytes: {}",
        BaseEncoding.base16().encode(binaryAuthMessage.getPayload().array())
      );
      this.sendMessage(binaryAuthMessage);
      return this.registerPendingResponse(btpAuthMessage.getRequestId())
        .thenAccept((response) -> {
        });
    } catch (InterruptedException | ExecutionException e) {
      return this.disconnect();
    }
  }

  @Override
  public CompletableFuture<Void> doDisconnect() {
    synchronized (webSocketSession) {
      // Block on the Disconnect...
      webSocketSession.ifPresent(session -> {
        try {
          session.close();
          webSocketSession = Optional.empty();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
    return CompletableFuture.completedFuture(null);
  }

}
