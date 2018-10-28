package com.sappenin.ilpv4.plugins.btp.spring;

import com.sappenin.ilpv4.plugins.btp.ws.ServerWebsocketBtpPlugin;
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
 */
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
    // TODO: What does the other side of the Websocket see if there's an exception here?

    this.serverWebsocketBtpPlugin.onIncomingBinaryMessage(webSocketSession, binaryMessage)
      .ifPresent(response -> {
        try {
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