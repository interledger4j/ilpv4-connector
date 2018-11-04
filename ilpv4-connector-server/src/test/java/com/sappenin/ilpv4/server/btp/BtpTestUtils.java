package com.sappenin.ilpv4.server.btp;

import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Random;

import static org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry.*;

public class BtpTestUtils {

  public static final int LATCH_LOCK_TIMEOUT = 3;
  public static final InterledgerAddress LOCAL_ILP_ADDRESS = InterledgerAddress.of("test.btp-server");
  public static final InterledgerAddress REMOTE_ILP_ADDRESS = InterledgerAddress.of("test.peer");

  public static final String TEST_AUTH_USERNAME = REMOTE_ILP_ADDRESS.getValue();
  public static final String TEST_AUTH_TOKEN = "shh";

  private final Random random;
  private final CodecContext btpCodecContext;
  private final CodecContext ilpCodecContext;

  public BtpTestUtils(final CodecContext ilpCodecContext, final CodecContext btpCodecContext) {
    this.btpCodecContext = Objects.requireNonNull(btpCodecContext);
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    this.random = new SecureRandom();
  }

  /**
   * Returns the next random request id using a PRNG.
   *
   * @return
   */
  public long nextRequestId() {
    return Math.abs(random.nextInt());
  }

  /**
   * An authentication message must have as its primary <tt>protocolData</tt> entry must have the name of 'auth',
   * content type <tt>MIME_APPLICATION_OCTET_STREAM</tt>, and empty data, and among the secondary entries, there MUST be
   * a UTF-8 'auth_token' entry.
   *
   * @return
   */
  public BtpMessage constructAuthMessage(final long requestId) {
    return constructAuthMessage(requestId, "test.foo", "password");
  }

  /**
   * An authentication message must have as its primary <tt>protocolData</tt> entry must have the name of 'auth',
   * content type <tt>MIME_APPLICATION_OCTET_STREAM</tt>, and empty data, and among the secondary entries, there MUST be
   * a UTF-8 'auth_token' entry.
   *
   * @return
   */
  public BtpMessage constructAuthMessage(final long requestId, final String username, final String authToken) {

    final BtpSubProtocol authSubProtocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .build();

    final BtpSubProtocol authUsernameSubprotocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH_USERNAME)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data(username.getBytes(StandardCharsets.UTF_8))
      .build();

    // In situations where no authentication is needed, the 'auth_token' data can be set to the empty string,
    // but it cannot be omitted.
    final BtpSubProtocol authTokenSubprotocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH_TOKEN)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data(authToken.getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocols btpSubProtocols = BtpSubProtocols.fromPrimarySubProtocol(authSubProtocol);
    btpSubProtocols.add(authUsernameSubprotocol);
    btpSubProtocols.add(authTokenSubprotocol);

    return BtpMessage.builder()
      .requestId(requestId)
      .subProtocols(btpSubProtocols)
      .build();
  }

  /**
   * An authentication message must have as its primary <tt>protocolData</tt> entry must have the name of 'auth',
   * content type <tt>MIME_APPLICATION_OCTET_STREAM</tt>, and empty data, and among the secondary entries, there MUST be
   * a UTF-8 'auth_token' entry.
   *
   * @return
   */
  public BtpMessage constructAuthMessageWithNoAuthToken(final long requestId) {

    final BtpSubProtocol authSubProtocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .build();

    final BtpSubProtocol authUsernameSubprotocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH_USERNAME)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data(REMOTE_ILP_ADDRESS.getValue().getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocols btpSubProtocols = BtpSubProtocols.fromPrimarySubProtocol(authSubProtocol);
    btpSubProtocols.add(authUsernameSubprotocol);

    return BtpMessage.builder()
      .requestId(requestId)
      .subProtocols(btpSubProtocols)
      .build();
  }

  /**
   * An authentication message must have as its primary <tt>protocolData</tt> entry must have the name of 'auth',
   * content type <tt>MIME_APPLICATION_OCTET_STREAM</tt>, and empty data, and among the secondary entries, there MUST be
   * a UTF-8 'auth_token' entry.
   *
   * @return
   */
  public BtpMessage constructAuthMessageWithNoAuthUsername(final long requestId) {

    final BtpSubProtocol authSubProtocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .build();

    // In situations where no authentication is needed, the 'auth_token' data can be set to the empty string,
    // but it cannot be omitted.
    final BtpSubProtocol authTokenSubprotocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH_TOKEN)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data(TEST_AUTH_TOKEN.getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocols btpSubProtocols = BtpSubProtocols.fromPrimarySubProtocol(authSubProtocol);
    btpSubProtocols.add(authTokenSubprotocol);

    return BtpMessage.builder()
      .requestId(requestId)
      .subProtocols(btpSubProtocols)
      .build();
  }

  /**
   * An authentication message must have as its primary <tt>protocolData</tt> entry must have the name of 'auth',
   * content type <tt>MIME_APPLICATION_OCTET_STREAM</tt>, and empty data, and among the secondary entries, there MUST be
   * a UTF-8 'auth_token' entry.
   *
   * @return
   */
  public BtpMessage constructIlpPrepareMessage(final long requestId, final InterledgerAddress interledgerAddress) {
    try {
      final byte[] randomBytes = new byte[32];
      new Random().nextBytes(randomBytes);
      final InterledgerCondition executionCondition = InterledgerCondition.of(randomBytes);

      final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .amount(BigInteger.TEN)
        .executionCondition(executionCondition)
        .destination(interledgerAddress)
        .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES)).build();

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      btpCodecContext.write(preparePacket, baos);

      final BtpSubProtocol ilpSubProtocol = BtpSubProtocol.builder()
        .protocolName(BTP_SUB_PROTOCOL_ILP)
        .data(baos.toByteArray())
        .build();

      final BtpSubProtocols btpSubProtocols = BtpSubProtocols.fromPrimarySubProtocol(ilpSubProtocol);

      return BtpMessage.builder()
        .requestId(requestId)
        .subProtocols(btpSubProtocols)
        .build();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
