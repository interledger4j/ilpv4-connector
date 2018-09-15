package com.sappenin.ilpv4.plugins.btp;

import com.sappenin.ilpv4.plugins.btp.converters.BinaryMessageToBtpMessageConverter;
import com.sappenin.ilpv4.plugins.btp.converters.BtpPacketToBinaryMessageConverter;
import org.immutables.value.Value;
import org.interledger.btp.*;
import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.sappenin.ilpv4.plugins.btp.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH_TOKEN;
import static com.sappenin.ilpv4.plugins.btp.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH_USERNAME;
import static org.interledger.btp.BtpErrorCode.F00_NotAcceptedError;

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
  private final BinaryMessageToBtpMessageConverter binaryMessageToBtpMessageConverter;
  private final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  public BtpSocketHandler(final CodecContext codecContext, final BtpSubProtocolHandlerRegistry registry,
                          final BinaryMessageToBtpMessageConverter binaryMessageToBtpMessageConverter,
                          final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter) {
    this.registry = Objects.requireNonNull(registry);
    this.codecContext = Objects.requireNonNull(codecContext);
    this.binaryMessageToBtpMessageConverter = Objects.requireNonNull(binaryMessageToBtpMessageConverter);
    this.btpPacketToBinaryMessageConverter = Objects.requireNonNull(btpPacketToBinaryMessageConverter);
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
    final BtpMessage incomingBtpMessage = getBtpMessage(binaryMessage);

    final BtpSubProtocols responses = new BtpSubProtocols();

    // For each subProtocol in the incoming message, handle it by mapping to an appropriate subProtocol response.
    incomingBtpMessage.getSubProtocols().forEach(btpSubProtocol -> {

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

    try {
      final BtpResponse btpResponse = BtpResponse.builder()
        .requestId(incomingBtpMessage.getRequestId())
        .subProtocols(responses)
        .build();
      webSocketSession.sendMessage(btpPacketToBinaryMessageConverter.convert(btpResponse));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);

      // Respond with a BTP Error on the websocket session.
      final BtpError btpError = BtpError.builder()
        .requestId(incomingBtpMessage.getRequestId())
        .errorCode(F00_NotAcceptedError)
        .build();
      webSocketSession.sendMessage(btpPacketToBinaryMessageConverter.convert(btpError));
    }

  }

  private void authenticate(final WebSocketSession webSocketSession, final BinaryMessage incomingBinaryMessage) throws IOException {
    Objects.requireNonNull(webSocketSession);
    Objects.requireNonNull(incomingBinaryMessage);

    final BtpMessage incomingBtpMessage = getBtpMessage(incomingBinaryMessage);

    try {
      final String auth_user = incomingBtpMessage.getSubProtocol(BTP_SUB_PROTOCOL_AUTH_USERNAME)
        .map(BtpSubProtocol::getDataAsString)
        .orElseThrow(() -> new RuntimeException(String.format("Expected BTP SubProtocol with Id: %s",
          BTP_SUB_PROTOCOL_AUTH_USERNAME)));

      final String auth_token = incomingBtpMessage.getSubProtocol(BTP_SUB_PROTOCOL_AUTH_TOKEN)
        .map(BtpSubProtocol::getDataAsString)
        .orElseThrow(() -> new RuntimeException(String.format("Expected BTP SubProtocol with Id: %s",
          BTP_SUB_PROTOCOL_AUTH_TOKEN)));

      this.storeAuthInWebSocketSession(webSocketSession, auth_user, auth_token);

      // Respond with a proper response...
      final BtpResponse btpResponse = this.constructAuthResponse(incomingBtpMessage.getRequestId());
      webSocketSession.sendMessage(btpPacketToBinaryMessageConverter.convert(btpResponse));

    } catch (RuntimeException e) {
      logger.error(e.getMessage(), e);

      // Response with a BTP Error on the websocket session.
      final BtpError btpError = this.constructAuthError(incomingBtpMessage.getRequestId(), e.getMessage());
      webSocketSession.sendMessage(btpPacketToBinaryMessageConverter.convert(btpError));
    }
  }

  /**
   * Construct a {@link BtpError} for the supplied request-id that can be returned when authentication is invalid.
   *
   * @param requestId
   *
   * @return
   */
  private BtpError constructAuthError(final long requestId, final String errorMessage) {
    return BtpError.builder()
      .requestId(requestId)
      .errorCode(F00_NotAcceptedError)
      .errorData(errorMessage.getBytes())
      .build();
  }

  /**
   * Construct a {@link BtpResponse} for the supplied request-id that can be returned when authentication has succeede.
   *
   * @param requestId
   *
   * @return
   */
  private BtpResponse constructAuthResponse(final long requestId) {
    return BtpResponse.builder()
      .requestId(requestId)
      .subProtocols(new BtpSubProtocols())
      .build();
  }

  private BtpMessage getBtpMessage(final BinaryMessage binaryMessage) throws IOException {
    final ByteBuffer buffer = Objects.requireNonNull(binaryMessage).getPayload();
    final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
    return codecContext.read(BtpMessage.class, stream);
  }

  //    private BinaryMessage getBinaryMessage(final BtpPacket packet) throws IOException {
  //      Objects.requireNonNull(packet);
  //      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
  //      codecContext.write(packet, baos);
  //      return new BinaryMessage(baos.toByteArray());
  //    }

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

    Objects.requireNonNull(webSocketSession).getAttributes().put(BtpSession.CREDENTIALS_KEY,
      ImmutableWebSocketCredentials.builder()
        .username(InterledgerAddress.of(username))
        .token(token)
        .build()
    );
  }

  private boolean isAuthenticated(final WebSocketSession webSocketSession) {
    return Objects.requireNonNull(webSocketSession).getAttributes().containsKey(BtpSession.CREDENTIALS_KEY);
  }

  @Value.Immutable
  interface WebSocketCredentials {
    InterledgerAddress username();

    String token();
  }
}