package com.sappenin.ilpv4.packetswitch.preemptors;

import org.interledger.core.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

import static org.interledger.core.InterledgerErrorCode.F01_INVALID_PACKET;

/**
 * <p>Implements the <tt>echo</tt> ILP Sub-protocol, which merely echoes back a fulfillment to the caller in order to
 * simulate something like <tt>ping</tt>.</p>
 *
 * <p>This controller is only ever engaged if a packet is addressed to _this_ Connector, so we don't actually need
 * to route this packet anywhere. Ping-protocol requests that are addressed to other nodes would not enter this
 * controller, and instead would be forwarded to the correct destination, if possible.</p>
 *
 * @see "https://github.com/interledger/rfcs/pull/232"
 * @deprecated This class is currently deprecated and will be replaced with a proper ping-protocol. Some problems with
 * this implementation: 1.) The protocol uses to payments to indicate connectivity. However, this is not always the
 * case. For example, a receiver may not be able to actually send 0-value payments. Instead, it would be better to have
 * a "ping" that is a 0-value payment with any condition. The "pong" can basically be an ILP fulfill (0-value) or a
 * reject (doesn't matter which) with some ILP data in it that contains anything necessary for the payload. 2.) the
 * protocol uses a wonky destination address called "ECHO" in the ILP packet. This is to allow this packet to be
 * forwarded to any participant in a payment chain. Instead of relying upon this special internal-prefix, we should
 * create a first-class allocation scheme that allows this to be used. We obviously have the need to be able to send
 * non-ILP messages to other ILP nodes in the network (without involving a payment). Counter-point is that these "ping"
 * request _should_ involve a very small amount of money, in which case we need some other indicator in the ILP
 * sub-protocol data, but probably should not be "ECHO".
 */
@Deprecated
public class EchoController {

  public static final String ECHO_DATA_PREFIX = "ECHOECHOECHOECHO";

  private static final int MINIMUM_ECHO_PACKET_DATA_LENGTH = 16 + 1;
  private static final int ECHO_DATA_PREFIX_LENGTH = ECHO_DATA_PREFIX.length();
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final CodecContext oerCodecContext;

  public EchoController(final CodecContext oerCodecContext) {
    this.oerCodecContext = Objects.requireNonNull(oerCodecContext);
  }

  /**
   * Handles an incoming ILP fulfill packet.
   *
   * @param sourceAccountAddress
   * @param sourcePreparePacket
   *
   * @return A {@link InterledgerPreparePacket} that can be sent back to the sender of the original ping packet.
   */
  public InterledgerPreparePacket handleIncomingData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) throws InterledgerProtocolException {

    logger.debug("Handling Incoming IlpPreparePacket from `{}` for Echo: {}",
      sourceAccountAddress, sourcePreparePacket);

    final int dataLength = sourcePreparePacket.getData().length;

    if (dataLength < MINIMUM_ECHO_PACKET_DATA_LENGTH) {
      throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
        .code(F01_INVALID_PACKET)
        .triggeredBy(sourceAccountAddress)
        .message("Incoming IlpPacket data too short for echo request! length=" + dataLength)
        .build());
    }

    // Doesn't copy bytes...
    final ByteBuffer prefixByteBuffer = ByteBuffer.wrap(sourcePreparePacket.getData(), 0, 16);
    // Copies bytes...
    //final byte[] prefixBytes = Arrays.copyOfRange(sourcePreparePacket.getData(), 0, 16);
    if (!prefixByteBuffer.toString().equals(ECHO_DATA_PREFIX)) {
      throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
        .code(F01_INVALID_PACKET)
        .triggeredBy(sourceAccountAddress)
        .message("Incoming IlpPacket data does not start with ECHO prefix!")
        .build());
    }

    final ByteBuffer echoPayload = ByteBuffer.wrap(sourcePreparePacket.getData(), ECHO_DATA_PREFIX_LENGTH,
      dataLength - ECHO_DATA_PREFIX_LENGTH);

    try {
      final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(echoPayload.array());
      short echoProtocolType = oerCodecContext.read(Short.class, byteArrayInputStream);

      if (echoProtocolType == 0) {

        final InterledgerAddress sourceAddressFromPacket =
          oerCodecContext.read(InterledgerAddress.class, byteArrayInputStream);

        logger.debug("Responding to ILP ping. sourceAccount={} sourceAddress={} cond={]",
          sourceAccountAddress, sourceAddressFromPacket,
          Base64.getEncoder().encodeToString(sourcePreparePacket.getExecutionCondition().getHash())
        );

        // Send an Echo payment back to the caller containing the "Pong Payment"...
        //        final UUID pingId = UUID.randomUUID();
        //        final String pingData =
        //          String.format("%s\n%s\n%s", ECHO_DATA_PREFIX, pingId, sourceAccountAddress.getValue());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(18);
        //byteArrayOutputStream.write(ECHO_DATA_PREFIX.getBytes(US_ASCII));
        oerCodecContext.write(ECHO_DATA_PREFIX, byteArrayOutputStream);
        oerCodecContext.write((short) 00, byteArrayOutputStream);

        //final String pingData = String.format("%s\n%s%s", ECHO_DATA_PREFIX, 00, 00);

        // TODO: Extract echo functionality into its own module or package or project, perhaps.
        final InterledgerPreparePacket echoPacket = InterledgerPreparePacket.builder()
          .destination(sourceAccountAddress)
          .expiresAt(Instant.now().plusSeconds(120))
          .executionCondition(InterledgerCondition.of(new byte[32]))
          .data(byteArrayOutputStream.toByteArray())
          .build();

        return echoPacket;
      } else {
        throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
          .code(F01_INVALID_PACKET)
          .triggeredBy(sourceAccountAddress)
          .message("Incoming IlpPacket contained unexpected ping request!")
          .build());
      }
    } catch (IOException e) {
      logger.error("Received unexpected ping request! sourceAccount={} ilpPacket: {}", sourceAccountAddress,
        sourcePreparePacket);
      throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
        .code(F01_INVALID_PACKET)
        .triggeredBy(sourceAccountAddress)
        .message("Incoming IlpPacket data does not have proper ECHO payload!")
        .build());
    }
  }
}
