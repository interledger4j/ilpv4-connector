package com.sappenin.ilpv4.plugins.btp.subprotocols.ilp;

import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import com.sappenin.ilpv4.plugins.btp.BtpSession;
import com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandler;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.core.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.support.Completions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * An extension of {@link BtpSubProtocolHandler} for handling the ILP sub-protocol. This implementation essentially
 * converts from a BTP message into an ILPv4 primitive, and then forwards the {@link IlpPacketSwitch} for processing by
 * the Connector.
 */
public class IlpBtpSubprotocolHandler extends BtpSubProtocolHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final CodecContext ilpCodecContext;

  // To avoid circular-dependencies, this handler must be set _after_ the Connector server has started...
  private Plugin.IlpDataHandler ilpPluginDataHandler;

  public IlpBtpSubprotocolHandler(final CodecContext ilpCodecContext) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
  }

  /**
   * Construct a {@link BtpSubProtocol} instance for using the <tt>ILP</tt> sub-protocol by storing the supplied {@code
   * preparePacket} into a BtpSubProtocol packet for transmission via BTP.
   *
   * @param ilpPacket       An {@link InterledgerPacket} that can be a prepare, fulfill, or error packet.
   * @param ilpCodecContext
   *
   * @return A {@link BtpSubProtocol}
   *
   * @deprecated Convert this to a {@link org.springframework.core.convert.converter.Converter}.
   */
  @Deprecated
  public static BtpSubProtocol ilp(
    final InterledgerPacket ilpPacket, final CodecContext ilpCodecContext
  ) {
    Objects.requireNonNull(ilpPacket);
    Objects.requireNonNull(ilpCodecContext);
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ilpCodecContext.write(ilpPacket, baos);

      return BtpSubProtocol.builder()
        .protocolName(BtpSubProtocols.INTERLEDGER)
        .contentType(BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM)
        .data(baos.toByteArray())
        .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Construct an {@link InterledgerFulfillPacket} by converting the data in the supplied BTP packet.
   *
   * @param btpResponse     A {@link BtpResponse} containing fulfill or reject packet.
   * @param ilpCodecContext A {@link CodecContext} that can read ILP Packets.
   *
   * @return An {@link InterledgerPacket} that was decoded from the supplied btp-response.
   *
   * @deprecated Convert this to a {@link org.springframework.core.convert.converter.Converter}.
   */
  @Deprecated
  public static InterledgerPacket toIlpPacket(
    BtpResponse btpResponse, final CodecContext ilpCodecContext
  ) {
    Objects.requireNonNull(btpResponse);
    Objects.requireNonNull(ilpCodecContext);

    try {
      final ByteArrayInputStream inputStream =
        new ByteArrayInputStream(btpResponse.getPrimarySubProtocol().getData());
      return ilpCodecContext.read(InterledgerPacket.class, inputStream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // Handles a BTP packet by converting subProtocolData into a typed object that can be properly handled in Java by
  // forwaring to the connector packetswitch.
  @Override
  public CompletableFuture<BtpSubProtocol> handleBinaryMessage(final BtpSession btpSession, final BtpMessage btpMessage) {
    Objects.requireNonNull(btpSession, "btpSession must not be null!");
    Objects.requireNonNull(btpMessage, "btpMessage must not be null!");
    Objects.requireNonNull(ilpPluginDataHandler,
      "ilpPluginDataHandler must be initialized before handling binary messages!"
    );

    if (logger.isDebugEnabled()) {
      logger.debug("Handling BTP Message: {}", btpMessage);
    }

    // TODO: Check for authentication. If not authenticated, then throw an exception! The Auth sub-protocol should
    // have been the first thing called for this BtpSession.
    // btpSession.isAuthenticated?

    try {
      // TODO: Use converter?
      // Convert to an ILP Prepare packet.
      final InterledgerPreparePacket incomingPreparePacket = ilpCodecContext
        .read(InterledgerPreparePacket.class, new ByteArrayInputStream(btpMessage.getPrimarySubProtocol().getData()));

      // For now, we assume that a peer will operate a lpi2 for each account that it owns, and so will initiate a BTP
      // connection for each account transmitting value because the the protocol is designed to have a single to/from in
      // it. To explore this further, consider the to/from sub-protocols proposed by Michiel, although these may not
      // be necessary, depending on actual deployment models.
      final InterledgerAddress sourceAccountId = btpSession.getPeerAccountAddress();

      // Forward the packet to the DataHandler for processing...
      return this.ilpPluginDataHandler.handleIncomingData(sourceAccountId, incomingPreparePacket)
        .thenApply(packet -> IlpBtpSubprotocolHandler.ilp(packet, ilpCodecContext));
    } catch (IOException e) {
      // Per RFC-23, ILP packets are attached under the protocol name "ilp" with content-type "application/octet-stream"
      // If an unreadable BTP packet is received, no response should be sent. An unreadable BTP packet is one which is
      // structurally invalid, i.e. terminates before length prefixes dictate or contains illegal characters.
      throw new RuntimeException(e);
    } catch (InterledgerProtocolException e) {
      return Completions.supplyAsync(() -> ilp(e.getInterledgerRejectPacket(), ilpCodecContext)).toCompletableFuture();
    }
  }

  @Lazy
  public void setIlpPluginDataHandler(final Plugin.IlpDataHandler ilpPluginDataHandler) {
    this.ilpPluginDataHandler = Objects.requireNonNull(ilpPluginDataHandler);
  }
}
