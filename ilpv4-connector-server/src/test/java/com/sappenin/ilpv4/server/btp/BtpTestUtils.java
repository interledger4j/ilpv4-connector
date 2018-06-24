package com.sappenin.ilpv4.server.btp;

import org.interledger.btp.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.web.socket.BinaryMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

import static com.sappenin.ilpv4.server.btp.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH;
import static com.sappenin.ilpv4.server.btp.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH_TOKEN;
import static com.sappenin.ilpv4.server.btp.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH_USERNAME;

public class BtpTestUtils {

  private final CodecContext codecContext;
  private final Random random;

  public BtpTestUtils(final CodecContext codecContext) {
    this.codecContext = Objects.requireNonNull(codecContext);
    this.random = new SecureRandom();
  }

  /**
   * Returns the next random request id using a PRNG.
   *
   * @return
   */
  public long nextRequestId() {
    return random.nextInt();
  }

  /**
   * Convert a {@link BinaryMessage} into a {@link BtpMessage}.
   *
   * @param binaryMessage
   *
   * @return
   *
   * @throws IOException
   */
  public BtpMessage toBtpMessage(final BinaryMessage binaryMessage) throws IOException {
    final ByteBuffer buffer = Objects.requireNonNull(binaryMessage, "binaryMessage must not be null!").getPayload();
    final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
    return codecContext.read(BtpMessage.class, stream);
  }

  /**
   * Convert a {@link BinaryMessage} into a {@link BtpResponse}.
   *
   * @param binaryMessage
   *
   * @return
   *
   * @throws IOException
   */
  public BtpResponse toBtpResponse(final BinaryMessage binaryMessage) throws IOException {
    final ByteBuffer buffer = Objects.requireNonNull(binaryMessage, "binaryMessage must not be null!").getPayload();
    final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
    return codecContext.read(BtpResponse.class, stream);
  }

  public BinaryMessage toBinaryMessage(final BtpMessage btpMessage) throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    this.codecContext.write(btpMessage, os);
    final byte[] payload = os.toByteArray();

    return new BinaryMessage(payload);
  }

  /**
   * An authentication message must have as its primary <tt>protocolData</tt> entry must have the name of 'auth',
   * content type <tt>MIME_APPLICATION_OCTET_STREAM</tt>, and empty data, and among the secondary entries, there MUST be
   * a UTF-8 'auth_token' entry.
   *
   * @return
   */
  public BtpMessage constructAuthMessage(final long requestId) {

    final BtpSubProtocol authSubProtocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH)
      .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
      //.data(new byte[0])
      .build();

    // In situations where no authentication is needed, the 'auth_token' data can be set to the empty string,
    // but it cannot be omitted.
    final BtpSubProtocol authTokenSubprotocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH_TOKEN)
      .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
      .data("password".getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocol authUsernameSubprotocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH_USERNAME)
      .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
      .data("test.foo".getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocols btpSubProtocols = BtpSubProtocols.fromPrimarySubProtocol(authSubProtocol);
    btpSubProtocols.add(authUsernameSubprotocol);
    btpSubProtocols.add(authTokenSubprotocol);

    return BtpMessage.builder()
      .requestId(requestId)
      .subProtocols(btpSubProtocols)
      .build();
  }
}
