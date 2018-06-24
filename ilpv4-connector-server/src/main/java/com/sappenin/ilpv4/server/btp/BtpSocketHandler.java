package com.sappenin.ilpv4.server.btp;

import org.interledger.btp.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

import static com.sappenin.ilpv4.server.btp.BtpSubProtocolHandlerRegistry.*;

/**
 * An extension of {@link BinaryWebSocketHandler} that handles BTP messages.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer
 * -protocol.md"
 */
public class BtpSocketHandler extends BinaryWebSocketHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final CodecContext codecContext;
  private final BtpSubProtocolHandlerRegistry registry;

  public BtpSocketHandler(final CodecContext codecContext, final BtpSubProtocolHandlerRegistry registry) {
    this.registry = Objects.requireNonNull(registry);
    this.codecContext = Objects.requireNonNull(codecContext);
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    //the messages will be broadcasted to all users.
    //sessions.add(session);
    // TODO: Add a Plugin here?
  }

  @Override
  public void handleBinaryMessage(final WebSocketSession webSocketSession, final BinaryMessage binaryMessage)
    throws IOException {

    if (!this.isAuthenticated(webSocketSession)) {
      authenticate(webSocketSession, binaryMessage);
      return;
    }

    final BtpSession btpSession = new BtpSession(webSocketSession);
    final BtpMessage btpMessage = getBtpMessage(binaryMessage);

    final BtpSubProtocols responses = new BtpSubProtocols();

    // For each subProtocol in the incoming message, handle it by mapping to an appropriate subProtocol response.
    btpMessage.getSubProtocols().forEach(btpSubProtocol -> {

      final BtpSubProtocolHandler handler = this.registry.getHandler(btpSubProtocol.getProtocolName())
        .orElseThrow(() -> new RuntimeException(
          String.format("No BTP Handler registered for BTP SubProtocol: %s", btpSubProtocol.getProtocolName()))
        );

      switch (btpSubProtocol.getContentType()) {
        case MIME_APPLICATION_OCTET_STREAM:
          byte[] binaryResponse = handler.handleBinaryMessage(btpSession, btpSubProtocol.getData());
          responses.add(BtpSubProtocol.builder()
            .protocolName(btpSubProtocol.getProtocolName())
            .contentType(BtpSubProtocolContentType.MIME_APPLICATION_OCTET_STREAM)
            .data(binaryResponse)
            .build()
          );
          break;

        case MIME_TEXT_PLAIN_UTF8:
          String textResponse = handler.handleTextMessage(btpSession, btpSubProtocol.getDataAsString());
          responses.add(BtpSubProtocol.builder()
            .protocolName(btpSubProtocol.getProtocolName())
            .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
            .data(textResponse.getBytes(StandardCharsets.UTF_8))
            .build()
          );
          break;

        case MIME_APPLICATION_JSON:
          String jsonResponse = handler.handleJsonMessage(btpSession, btpSubProtocol.getDataAsString());
          responses.add(BtpSubProtocol.builder()
            .protocolName(btpSubProtocol.getProtocolName())
            .contentType(BtpSubProtocolContentType.MIME_APPLICATION_JSON)
            .data(jsonResponse.getBytes(StandardCharsets.UTF_8))
            .build()
          );
          break;
      }
    });

    webSocketSession.sendMessage(getBinaryMessage(
      BtpResponse.builder()
        .requestId(btpMessage.getRequestId())
        .subProtocols(responses)
        .build()
    ));

  }

  private void authenticate(final WebSocketSession webSocketSession, final BinaryMessage binaryMessage) throws IOException {
    Objects.requireNonNull(webSocketSession);
    Objects.requireNonNull(binaryMessage);

    final BtpMessage btpMessage = getBtpMessage(binaryMessage);

    if (btpMessage.hasSubProtocol(BTP_SUB_PROTOCOL_AUTH)) {
      final String auth_user = btpMessage.getSubProtocol(BTP_SUB_PROTOCOL_AUTH_USERNAME)
        .map(BtpSubProtocol::getDataAsString)
        .orElseThrow(() -> new RuntimeException(String.format("Expected SubProtocol with Id: %s",
          BTP_SUB_PROTOCOL_AUTH_USERNAME)));

      final String auth_token = btpMessage.getSubProtocol(BTP_SUB_PROTOCOL_AUTH_TOKEN)
        .map(BtpSubProtocol::getDataAsString)
        .orElseThrow(() -> new RuntimeException(String.format("Expected SubProtocol with Id: %s",
          BTP_SUB_PROTOCOL_AUTH_TOKEN)));

      this.storeAuthInWebSocketSession(webSocketSession, auth_user, auth_token);

      webSocketSession.sendMessage(
        getBinaryMessage(
          BtpResponse.builder()
            .requestId(btpMessage.getRequestId())
            .subProtocols(new BtpSubProtocols())
            .build()
        )
      );
    } else {
      webSocketSession.sendMessage(getBinaryMessage(
        BtpError.builder()
          .requestId(btpMessage.getRequestId())
          .errorCode(BtpErrorCode.F00_NotAcceptedError)
          // TODO: Add to BtpErrorCode
          .errorName("NotAcceptedError")
          .errorData(new byte[]{})
          .triggeredAt(Instant.now())
          .build()
      ));
    }
  }

  private BtpMessage getBtpMessage(final BinaryMessage binaryMessage) throws IOException {
    final ByteBuffer buffer = Objects.requireNonNull(binaryMessage).getPayload();
    final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
    return codecContext.read(BtpMessage.class, stream);
  }

  private BinaryMessage getBinaryMessage(final BtpPacket packet) throws IOException {
    Objects.requireNonNull(packet);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    codecContext.write(packet, baos);
    return new BinaryMessage(baos.toByteArray());
  }

  /**
   * Store the username and token into this Websocket session.
   *
   * @param username The username of the signed-in account.
   * @param token    An authorization token used to authenticate the indicated user.
   */
  private void storeAuthInWebSocketSession(
    final WebSocketSession webSocketSession, final String username, final String token
  ) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(token);

    Objects.requireNonNull(webSocketSession).getAttributes().put(BtpSession.ACCOUNT_KEY, username + ":" + token);
  }

  private boolean isAuthenticated(final WebSocketSession webSocketSession) {
    return Objects.requireNonNull(webSocketSession).getAttributes().containsKey(BtpSession.ACCOUNT_KEY);
  }
}