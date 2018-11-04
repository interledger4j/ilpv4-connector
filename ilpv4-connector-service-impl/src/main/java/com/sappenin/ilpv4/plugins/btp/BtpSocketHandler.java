package com.sappenin.ilpv4.plugins.btp;

import org.interledger.plugin.lpiv2.btp2.spring.ServerWebsocketBtpPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Objects;

/**
 * <p>An extension of {@link BinaryWebSocketHandler} that translates between Spring's {@link BinaryMessage} into BTP's
 * concrete types, and then delegates to an instance of {@link ServerWebsocketBtpPlugin}.</p>
 *
 * <p>Note that all messages handled by this handler are processing using BTP, although multiple callers/sessions may
 * exist and be handled by this same handler.</p>
 *
 * @deprecated of Instead using this class to facilitate late-binding of a BTP Server plugin, we can remove this class
 * and instead always create a BtpServerPlugin Bean via spring. Then, we can Inject that instance into SpringWsConfig.
 * If Websockets are turned on, we can connect the injected plugin to the WebsocketHandler directly in the config. This
 * will drastically simplify the implementation.
 */
@Deprecated
public class BtpSocketHandler extends BinaryWebSocketHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private ServerWebsocketBtpPlugin serverWebsocketBtpPlugin;

  @Override
  public void afterConnectionEstablished(final WebSocketSession session) {
    Objects.requireNonNull(serverWebsocketBtpPlugin, "BTP over Websockets requires a ServerWebsocketBtpPlugin!");
    logger.debug("Incoming WS Client Connection Established: {}", session);

    // Set the Websocket session into the plugin...
    this.serverWebsocketBtpPlugin.setWebSocketSession(session);
  }

  @Override
  public void handleBinaryMessage(final WebSocketSession webSocketSession, final BinaryMessage binaryMessage) {
    Objects.requireNonNull(serverWebsocketBtpPlugin, "BTP over Websockets requires a ServerWebsocketBtpPlugin!");

    this.serverWebsocketBtpPlugin.onIncomingBinaryMessage(webSocketSession, binaryMessage)
      .ifPresent(response -> {
        try {
          // Return the response to the caller...
          // TODO: What does the other side of the Websocket see if there's an exception here?
          webSocketSession.sendMessage(response);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    logger.debug("Incoming WS Client Connection Closed: {}", session);
  }

  public void setServerWebsocketBtpPlugin(final ServerWebsocketBtpPlugin serverWebsocketBtpPlugin) {
    this.serverWebsocketBtpPlugin = Objects.requireNonNull(serverWebsocketBtpPlugin);
  }
}