package com.sappenin.ilpv4.plugins.btp.subprotocols.ilp;

import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import org.interledger.core.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.btp2.subprotocols.ilp.AbstractIlpBtpSubprotocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>An extension of {@link AbstractIlpBtpSubprotocolHandler} for handling the ILP sub-protocol.</p>
 *
 * <p>Generally, this implementation is only used to convert from a BTP message into an ILPv4 primitive, and then
 * forwards to the {@link IlpPacketSwitch} for processing by an IlpV4 Connector. However, any {@link
 * Plugin.IlpDataHandler} can be used.</p>
 */
public class ConnectorIlpBtpSubprotocolHandler extends AbstractIlpBtpSubprotocolHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // To avoid circular-dependencies, this handler must be set _after_ the Connector server has started...
  private Plugin.IlpDataHandler ilpPluginDataHandler;

  // To avoid circular-dependencies, this handler must be set _after_ the Connector server has started...
  private Plugin.IlpMoneyHandler ilpMoneyHandler;

  public ConnectorIlpBtpSubprotocolHandler(final CodecContext ilpCodecContext) {
    super(ilpCodecContext);
  }

  @Override
  public CompletableFuture<? extends InterledgerPacket> onIlpPacket(
    final InterledgerAddress sourceAccountId, final InterledgerPacket ilpPacket
  ) throws InterledgerProtocolException {

    // Given a generic IlpPacket, handle all variants properly.
    new InterledgerPacketHandler<CompletableFuture<InterledgerFulfillPacket>>() {

      @Override
      protected CompletableFuture<InterledgerPacket> handlePreparePacket(InterledgerPreparePacket interledgerPreparePacket) {
        return ilpPluginDataHandler.handleIncomingData(sourceAccountId, interledgerPreparePacket);
      }

      @Override
      protected CompletableFuture<InterledgerFulfillPacket> handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        // TODO: If a plugin has an incoming Fulfillment message, this is actually a response to a previous request,
        // so we need to bridge this response (fulfillment) back to the

        return null;
      }

      @Override
      protected CompletableFuture<InterledgerFulfillPacket> handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        return null;
      }
    }.handle(ilpPacket);







  }

  //  /**
  //   * Sends an ILP request packet to the peer and returns the response packet (this method correlates with
  //   * <tt>sendData</tt> in the Javascript connector).
  //   *
  //   * @param ilpPacket An {@link InterledgerPacket} for a corresponding BTP sub-protocol payload. Note that this may be
  //   *                  an ILP request (prepare packet) or a response (i.e., fulfillment or rejection).
  //   *
  //   * @return A {@link CompletableFuture} that resolves to the ILP response from the peer.
  //   *
  //   * @throws InterledgerProtocolException if the request is rejected by the peer.
  //   */
  //  @Override
  //  public CompletableFuture<Optional<InterledgerPacket>> onIlpPacket(InterledgerPacket ilpPacket) throws InterledgerProtocolException {
  //
  //
  //    return this.ilpPluginDataHandler.handleIncomingData(sourceAccountId, ilpPacket);
  //  }

  //  // Handles a BTP packet by converting subProtocolData into a typed object that can be properly handled in Java by
  //  // forwaring to the connector packetswitch.
  //  @Override
  //  public CompletableFuture<BtpSubProtocol> handleBinaryMessage(final BtpSession btpSession, final BtpMessage btpMessage) {
  //    Objects.requireNonNull(btpSession, "btpSession must not be null!");
  //    Objects.requireNonNull(btpMessage, "btpMessage must not be null!");
  //    Objects.requireNonNull(ilpPluginDataHandler,
  //      "ilpPluginDataHandler must be initialized before handling binary messages!"
  //    );
  //
  //    if (logger.isDebugEnabled()) {
  //      logger.debug("Handling BTP Message: {}", btpMessage);
  //    }
  //
  //    // TODO: Check for authentication. If not authenticated, then throw an exception! The Auth sub-protocol should
  //    // have been the first thing called for this BtpSession.
  //    // btpSession.isAuthenticated?
  //
  //    try {
  //      // TODO: Use converter?
  //      // Convert to an ILP Prepare packet.
  //      final InterledgerPreparePacket incomingPreparePacket = ilpCodecContext
  //        .read(InterledgerPreparePacket.class, new ByteArrayInputStream(btpMessage.getPrimarySubProtocol().getData()));
  //
  //      // For now, we assume that a peer will operate a lpi2 for each account that it owns, and so will initiate a BTP
  //      // connection for each account transmitting value because the the protocol is designed to have a single to/from in
  //      // it. To explore this further, consider the to/from sub-protocols proposed by Michiel, although these may not
  //      // be necessary, depending on actual deployment models.
  //      final InterledgerAddress sourceAccountId = btpSession.getPeerAccountAddress();
  //
  //      // Forward the packet to the DataHandler for processing...
  //      return this.ilpPluginDataHandler.handleIncomingData(sourceAccountId, incomingPreparePacket)
  //        .thenApply(packet -> IlpConnectorBtpSubprotocolHandler.ilp(packet, ilpCodecContext));
  //    } catch (IOException e) {
  //      // Per RFC-23, ILP packets are attached under the protocol name "ilp" with content-type "application/octet-stream"
  //      // If an unreadable BTP packet is received, no response should be sent. An unreadable BTP packet is one which is
  //      // structurally invalid, i.e. terminates before length prefixes dictate or contains illegal characters.
  //      throw new RuntimeException(e);
  //    } catch (InterledgerProtocolException e) {
  //      return Completions.supplyAsync(() -> ilp(e.getInterledgerRejectPacket(), ilpCodecContext)).toCompletableFuture();
  //    }
  //  }

  @Lazy
  public void setIlpPluginDataHandler(final Plugin.IlpDataHandler ilpPluginDataHandler) {
    this.ilpPluginDataHandler = Objects.requireNonNull(ilpPluginDataHandler);
  }

  @Lazy
  public void setIlpMoneyHandler(final Plugin.IlpMoneyHandler ilpMoneyHandler) {
    this.ilpMoneyHandler = Objects.requireNonNull(ilpMoneyHandler);
  }
}
