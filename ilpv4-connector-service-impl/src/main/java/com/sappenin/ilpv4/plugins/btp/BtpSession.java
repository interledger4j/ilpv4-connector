package com.sappenin.ilpv4.plugins.btp;

import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;
import java.util.Optional;

/**
 * A session for BTP.
 */
public class BtpSession {

  static final String CREDENTIALS_KEY = "ILP-BTP-Credentials";

  private final WebSocketSession webSocketSession;

  private final BtpSocketHandler.WebSocketCredentials webSocketCredentials;

  /**
   * Private constructor.
   *
   * @param webSocketSession
   */
  public BtpSession(final WebSocketSession webSocketSession) {
    this.webSocketSession = Objects.requireNonNull(webSocketSession);
    this.webSocketCredentials = Optional.ofNullable(
      Objects.requireNonNull(webSocketSession).getAttributes().get(CREDENTIALS_KEY)
    )
      .map(obj -> (BtpSocketHandler.WebSocketCredentials) obj)
      .orElseThrow(() -> new RuntimeException("No Credentials found in WebSocket Session!"));
  }

  public BtpSocketHandler.WebSocketCredentials getWebSocketCredentials() {
    return this.webSocketCredentials;
  }

  public WebSocketSession getWebSocketSession() {
    return webSocketSession;
  }
}
