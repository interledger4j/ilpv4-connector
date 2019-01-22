package com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp.connectorMode;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.accounts.BtpAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.btp.BtpError;
import org.interledger.btp.BtpErrorCode;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpRuntimeException;
import org.interledger.btp.BtpSession;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpTransfer;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.subprotocols.AbstractBtpSubProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>An extension of {@link AbstractBtpSubProtocolHandler} for handling incoming <tt>ILP</tt> sub-protocol messages
 * received over BTP, but in a multi-account environment.</p>
 *
 * <p>This implementation essentially converts from an incoming BTP message into an ILPv4 primitive and
 * exposes methods that can be implemented to handle these messages at the ILP-layer.</p>
 *
 * <p>NOTE: This implementation is appropriate for handling ILP packets in a multi plugin/account environment. For a
 * single-account environment, such as a BTP Client, consider {@link ConnectorModeIlpBtpSubprotocolHandler}
 * instead.</p>
 */
public class ConnectorModeIlpBtpSubprotocolHandler extends AbstractBtpSubProtocolHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  private final CodecContext ilpCodecContext;
  private final BtpAccountIdResolver accountIdResolver;
  private final ILPv4PacketSwitch dataHandler;

  /**
   * Required-args Constructor.
   *
   * @param ilpCodecContext      A {@link CodecContext} that can handle encoding/decoding of ILP Packets.
   * @param btpAccountIdResolver
   */
  public ConnectorModeIlpBtpSubprotocolHandler(
    final CodecContext ilpCodecContext,
    final BtpAccountIdResolver btpAccountIdResolver,
    final ILPv4PacketSwitch dataHandler
  ) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    this.accountIdResolver = Objects.requireNonNull(btpAccountIdResolver);
    this.dataHandler = Objects.requireNonNull(dataHandler);
  }

  @Override
  public CompletableFuture<Optional<BtpSubProtocol>> handleSubprotocolDataForBtpMessage(
    final BtpSession btpSession, final BtpMessage incomingBtpMessage
  ) {
    Objects.requireNonNull(btpSession, "btpSession must not be null!");
    Objects.requireNonNull(incomingBtpMessage, "incomingBtpMessage must not be null!");

    if (logger.isDebugEnabled()) {
      logger.debug("Incoming ILP Subprotocol BtpMessage: {}", incomingBtpMessage);
    }

    // Throws if there's an auth problem.
    if (!btpSession.isAuthenticated()) {
      throw new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, "BtpSession not authenticated!");
    }

    try {
      // Convert to an ILP Prepare packet.
      final InterledgerPacket incomingIlpPacket = ilpCodecContext
        .read(InterledgerPacket.class,
          new ByteArrayInputStream(incomingBtpMessage.getPrimarySubProtocol().getData()));

      // If the Packet is a prepare packet, then we forward to the ilpPlugin Data Handler which bridges to the software
      // that is listening for incoming ILP packet (typically this is the ILP Connector Switch, but might be client software).
      // This mapper should never encounter a Fulfill/Reject packet in this way. This is because when a Prepare packet
      // is sent out through a BTP session, a CompletableFuture is always constructed to accept the response and return it
      // to the original caller. Thus, if a Fulfill/Reject packet makes it into this location, it's an error.
      if (InterledgerPreparePacket.class.isAssignableFrom(incomingIlpPacket.getClass())) {
        final AccountId accountId = this.accountIdResolver.resolveAccountId(btpSession);
        final InterledgerPreparePacket interledgerPreparePacket = (InterledgerPreparePacket) incomingIlpPacket;
        return dataHandler
          // Handle the incoming ILP Data packet.
          .routeData(accountId, interledgerPreparePacket)
          .exceptionally(error -> {
            logger.error(error.getMessage(), error);
            return Optional.empty();
          })
          // and convert back to BTP...
          .thenApply((responsePacket) -> responsePacket
            .map($ -> org.interledger.plugin.lpiv2.btp2.subprotocols.ilp.IlpBtpSubprotocolHandler
              .toBtpSubprotocol($, ilpCodecContext))
            .map(Optional::ofNullable)
            .orElseGet(Optional::empty)
          );
      } else {
        logger.error("Encountered errant InterledgerResponsePacket but should not have: {}", incomingIlpPacket);
        throw new RuntimeException(
          String.format("Unsupported InterledgerPacket Type: %s", incomingIlpPacket.getClass()));
      }
    } catch (IOException e) {
      // Per RFC-23, ILP packets are attached under the protocol name "ilp" with content-type
      // "application/octet-stream". If an unreadable BTP packet is received, no response should be sent. An unreadable
      // BTP packet is one which is structurally invalid, i.e. terminates before length prefixes dictate or contains
      // illegal characters.
      logger.error("Unable to process incoming incomingBtpMessage as an InterledgerPacket: {}", incomingBtpMessage);
      return CompletableFuture.completedFuture(Optional.empty());
    }
  }

  @Override
  public CompletableFuture<Optional<BtpSubProtocol>> handleSubprotocolDataForBtpTransfer(
    final BtpSession btpSession, final BtpTransfer incomingBtpTransfer
  ) throws BtpRuntimeException {
    Objects.requireNonNull(btpSession);
    Objects.requireNonNull(incomingBtpTransfer);

    logger.debug("Incoming ILP Subprotocol BtpTransfer: {}", incomingBtpTransfer);
    return CompletableFuture.completedFuture(Optional.empty());
  }

  @Override
  public CompletableFuture<Void> handleSubprotocolDataForBtpResponse(
    final BtpSession btpSession, final BtpResponse incomingBtpResponse
  ) throws BtpRuntimeException {

    Objects.requireNonNull(btpSession);
    Objects.requireNonNull(incomingBtpResponse);

    logger.debug("Incoming ILP Subprotocol BtpResponse: {}", incomingBtpResponse);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> handleSubprotocolDataForBtpError(
    final BtpSession btpSession, final BtpError incomingBtpError
  ) throws BtpRuntimeException {

    Objects.requireNonNull(btpSession);
    Objects.requireNonNull(incomingBtpError);

    logger.error("Incoming ILP Subprotocol BtpError: {}", incomingBtpError);
    return CompletableFuture.completedFuture(null);
  }
}
