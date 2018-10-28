package com.sappenin.ilpv4.client.btp;

import org.interledger.btp.subprotocols.ilp.IlpBtpSubprotocolHandler;
import org.interledger.core.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * An extension of {@link IlpBtpSubprotocolHandler} that merely logs the
 */
public class LoggingIlpBtpSubprotocolHandler extends IlpBtpSubprotocolHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * No-args Constructor.
   *
   * @param ilpCodecContext
   */
  public LoggingIlpBtpSubprotocolHandler(final CodecContext ilpCodecContext) {
    super(ilpCodecContext);
  }

  /**
   * Sends an ILP request packet to the peer and returns the response packet (this method correlates with
   * <tt>sendData</tt> in the Javascript connector).
   *
   * @param incomingIlpPacket An {@link InterledgerPacket} received from a remote peer in a bilaterlal BTP connection.
   *                          Note that this may be an ILP request (e.g., prepare packet) or an ILP response (i.e.,
   *                          fulfillment or rejection).
   *
   * @return A {@link CompletableFuture} that resolves to the ILP response from the peer.
   *
   * @throws InterledgerProtocolException if the request is rejected by the peer.
   */
  @Override
  public CompletableFuture<Optional<InterledgerPacket>> onIlpPacket(final InterledgerPacket incomingIlpPacket) throws InterledgerProtocolException {
    Objects.requireNonNull(incomingIlpPacket);

    // Handle incoming packets by simply logging them...
    new InterledgerPacketHandler<Void>() {

      @Override
      protected Void handlePreparePacket(final InterledgerPreparePacket interledgerPreparePacket) {
        throw new RuntimeException("BTP Clients cannot process ILP Prepare packets!");
      }

      @Override
      protected Void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        logger.info("Payment Fulfilled: {}", interledgerFulfillPacket);
        return null;
      }

      @Override
      protected Void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        logger.error("Payment Rejected: {}", interledgerRejectPacket);
        return null;
      }
    }.handle(incomingIlpPacket);

    return CompletableFuture.completedFuture(Optional.empty());
  }

}
