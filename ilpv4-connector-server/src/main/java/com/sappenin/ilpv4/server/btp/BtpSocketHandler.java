package com.sappenin.ilpv4.server.btp;

import org.interledger.btp.*;
import org.interledger.btp.asn.framework.BtpCodecs;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
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

public class BtpSocketHandler extends BinaryWebSocketHandler {

  public static final String SUB_PROTOCOL_ID__AUTH = "auth";

  private final BtpSubProtocolHandlerRegistry registry;
  private final CodecContext context;

  public BtpSocketHandler(BtpSubProtocolHandlerRegistry registry) {
    this.registry = registry;
    this.context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES);
    BtpCodecs.register(context);
  }

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

    if (btpMessage.hasSubProtocol(SUB_PROTOCOL_ID__AUTH)) {
      //This should be an auth binaryMessage
      String auth_token = btpMessage.getSubProtocol("auth_token").getDataAsString();
      String auth_user = btpMessage.getSubProtocol("auth_username").getDataAsString();

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
    return context.read(BtpMessage.class, stream);
  }

  private BinaryMessage getBinaryMessage(final BtpPacket packet) throws IOException {
    Objects.requireNonNull(packet);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    context.write(packet, baos);
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