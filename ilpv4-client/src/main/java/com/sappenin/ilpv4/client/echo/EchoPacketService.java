package com.sappenin.ilpv4.client.echo;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

/**
 * A service that constructs packets for the ILPv4 Connector (informal) echo protocol.
 *
 * @see ""
 * @deprecated This class is deprecated and will be removed in a future version, once the Echo protocol is replaced with
 * a Ping protocol.
 */
public class EchoPacketService {
  public static final String ECHO_DATA_PREFIX = "ECHOECHOECHOECHO";

  private final CodecContext ilpCodecContext;

  public EchoPacketService(final CodecContext ilpCodecContext) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
  }

  /**
   * Construct a new {@link InterledgerPreparePacket} that contains an encoded set of data that conforms to the
   * <tt>echo</tt> protocol allowing a client to originate a <tt>ping</tt> request.
   *
   * @param sourceAddress      An {@link InterledgerAddress} of the client sending this echo/ping request.
   * @param destinationAddress An {@link InterledgerAddress} of the destination to be ping'd.
   *
   * @return
   */
  public InterledgerPreparePacket constructEchoRequestPacket(
    final InterledgerAddress sourceAddress,
    final InterledgerAddress destinationAddress) {
    Objects.requireNonNull(destinationAddress);

    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(18);

      byteArrayOutputStream.write(ECHO_DATA_PREFIX.getBytes(StandardCharsets.US_ASCII));
      ilpCodecContext.write((short) 0, byteArrayOutputStream);
      ilpCodecContext.write(sourceAddress, byteArrayOutputStream);

      return InterledgerPreparePacket.builder()
        .destination(destinationAddress)
        // Allow override?
        .expiresAt(Instant.now().plusSeconds(30))
        .executionCondition(randomFulfillment().getCondition())
        .data(byteArrayOutputStream.toByteArray())
        .build();

    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public InterledgerPreparePacket createEchoResponsePacket(final InterledgerAddress destinationAddress) {

    Objects.requireNonNull(destinationAddress);

    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(18);
      byteArrayOutputStream.write(ECHO_DATA_PREFIX.getBytes(StandardCharsets.US_ASCII));
      ilpCodecContext.write(0, byteArrayOutputStream);

      return InterledgerPreparePacket.builder()
        .destination(destinationAddress)
        // Allow override?
        .expiresAt(Instant.now().plusSeconds(30))
        .executionCondition(randomFulfillment().getCondition())
        .data(byteArrayOutputStream.toByteArray())
        .build();

    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  private InterledgerFulfillment randomFulfillment() {
    // Fill the array with 32 bytes...
    final byte[] preimageBytes = new byte[32];
    new SecureRandom().nextBytes(preimageBytes);
    return InterledgerFulfillment.of(preimageBytes);
  }
}
