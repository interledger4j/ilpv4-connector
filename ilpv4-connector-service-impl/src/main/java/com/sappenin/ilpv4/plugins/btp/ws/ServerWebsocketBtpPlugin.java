package com.sappenin.ilpv4.plugins.btp.ws;

import com.sappenin.ilpv4.plugins.btp.BtpServerPluginSettings;
import com.sappenin.ilpv4.plugins.btp.spring.BtpSocketHandler;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BinaryMessageToBtpPacketConverter;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BtpPacketToBinaryMessageConverter;
import com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.btp.BtpPacket;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpRuntimeException;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.PluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * An extension of {@link AbstractWebsocketBtpPlugin} for when the plugin is operating the Websocket server, accepting
 * connections from a remote BTP Plugin.
 */
public class ServerWebsocketBtpPlugin extends AbstractWebsocketBtpPlugin<BtpServerPluginSettings> {

  public static final String PLUGIN_TYPE_STRING = "BTP_SERVER";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientWebsocketBtpPlugin.class);

  private final ApplicationContext applicationContext;

  /**
   * Required-args Constructor.
   */
  public ServerWebsocketBtpPlugin(
    final BtpServerPluginSettings pluginSettings,
    final CodecContext ilpCodecContext,
    final CodecContext btpCodecContext,
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    //final IlpLogUtils ilpLogUtils,
    final ApplicationContext applicationContext
  ) {
    super(pluginSettings, ilpCodecContext, btpCodecContext, btpSubProtocolHandlerRegistry,
      binaryMessageToBtpPacketConverter, btpPacketToBinaryMessageConverter);

    this.applicationContext = Objects.requireNonNull(applicationContext);
    this.webSocketSession = Optional.empty();
  }

  /**
   * This method will be called for all plugins. This is a listening-only variant (i.e., no external connection needs to
   * be made), so this is a no-op.
   */
  @Override
  public CompletableFuture<Void> doConnect() {
    // Grab BtpSocketHandler from the Context, and call setServerWebsocketBtpPlugin(this) in order to connect the two.
    this.applicationContext.getBean(BtpSocketHandler.class).setServerWebsocketBtpPlugin(this);

    // There is else todo to connect this plugin to the server...if a client successfully connects via Auth, then a
    // webSocketSession will be attached.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> doDisconnect() {
    return CompletableFuture.runAsync(() -> {
      // Close the webSocketSession...
      this.webSocketSession.ifPresent(presentSession -> {
        try {
          presentSession.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      this.webSocketSession = Optional.empty();
    });
  }

  /**
   * Implemented to support BTP. When sending an outgoing packet, the plugin must connect the response to this request.
   *
   * @param btpPacket A {@link BtpPacket} to send to the remote peer.
   *
   * @return CompletableFuture<BtpResponse>
   */
  @Override
  protected CompletableFuture<BtpResponse> doSendDataOverBtp(final BtpPacket btpPacket) throws BtpRuntimeException {
    Objects.requireNonNull(btpPacket);

    // Send the BtpMessage to the remote peer using the websocket client....but first translate the BtpMessage to a
    // binary message.
    try {
      final BinaryMessage binaryMessage = this.btpPacketToBinaryMessageConverter.convert(btpPacket);
      // Send the message and return the pendingResponse...
      sendMessage(binaryMessage);

      // Register and return the response...
      return this.registerPendingResponse(btpPacket.getRequestId());
    } catch (Exception e) {
      throw new BtpRuntimeException(e);
    }
  }

  @Override
  public BtpPluginType getBtpPluginType() {
    return BtpPluginType.WS_SERVER;
  }

  /////////////////
  // Helper Methods
  /////////////////

  public void setWebSocketSession(final WebSocketSession session) {
    this.webSocketSession = Optional.of(session);
  }
}
