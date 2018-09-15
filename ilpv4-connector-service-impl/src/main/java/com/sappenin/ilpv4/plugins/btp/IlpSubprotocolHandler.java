package com.sappenin.ilpv4.plugins.btp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.IlpConnector;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocolContentType;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.core.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.interledger.btp.BtpSubProtocols.INTERLEDGER;

/**
 * An extension of {@link BtpSubProtocolHandler} for handling the ILP sub-protocol.
 */
public class IlpSubprotocolHandler extends BtpSubProtocolHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final CodecContext codecContext;
  private final IlpConnector ilpConnector;

  public IlpSubprotocolHandler(
    final CodecContext codecContext, final IlpConnector ilpConnector
  ) {
    this.codecContext = Objects.requireNonNull(codecContext);
    this.ilpConnector = Objects.requireNonNull(ilpConnector);
  }

  @Override
  public byte[] handleBinaryMessage(final BtpSession session, final byte[] data) {
    Objects.requireNonNull(session, "session must not be null!");
    Objects.requireNonNull(data, "data must not be null!");

    if (logger.isDebugEnabled()) {
      logger.debug(
        "Handling BTP data from {}: {}",
        BaseEncoding.base64().encode(data), session.getWebSocketCredentials().username()
      );
    }

    // TODO: Move this logic out of the BTP-specific code, and into the connector.

    // Per RFC-23, ILP packets are attached under the protocol name "ilp" with content-type "application/octet-getAllAccountSettings"
    try {
      // Convert to an ILP Prepare packet.
      final InterledgerPreparePacket incomingPreparePacket =
        codecContext.read(InterledgerPreparePacket.class, new ByteArrayInputStream(data));

      // For now, we assume that a peer will operate a lpi2 for each account that it owns, and so will initiate a BTP
      // connection for each account transmitting value.
      // TODO: However, BTP should ideally operate on a per-peer basis,
      // although the protocol is designed to have a single to/from in it. To explore this further, consider the
      // to/from sub-protocols proposed by Michiel.
      final InterledgerAddress sourceAccountId = session.getWebSocketCredentials().username();
      return this.ilpConnector.handleIncomingData(sourceAccountId, incomingPreparePacket)
        .thenApply(this::toBtpResponse)
        // TODO: Change this to be a configurable timeout!
        .get(30, TimeUnit.SECONDS);

    } catch (InterledgerProtocolException e) {
      // TODO: Return an appropriate BTP error message with an ILP Error message as data, per https://github
      // .com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md
      throw new RuntimeException(e);
    } catch (IOException | RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
      // TODO: Return an appropriate BTP error message per https://github
      // .com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  protected final byte[] toBtpResponse(final InterledgerFulfillPacket fulfillPacket) {
    Objects.requireNonNull(fulfillPacket);
    try {
      final BtpSubProtocols btpSubProtocols = BtpSubProtocols.fromPrimarySubProtocol(btpSubProtocol(fulfillPacket));
      final BtpResponse btpResponse = BtpResponse.builder().requestId(1L).subProtocols(btpSubProtocols).build();
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      codecContext.write(btpResponse, os);
      return os.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Constructs a BtpSubProtocol object from an Interledger packet.
   *
   * @param preparePacket An instance of {@link InterledgerPacket}.
   *
   * @return An instance of {@link BtpSubProtocol}.
   *
   * @throws IOException
   */
  @VisibleForTesting
  protected final BtpSubProtocol btpSubProtocol(InterledgerPacket preparePacket) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    codecContext.write(preparePacket, out);

    return BtpSubProtocol.builder()
      .protocolName(INTERLEDGER)
      .contentType(BtpSubProtocolContentType.MIME_APPLICATION_OCTET_STREAM)
      .data(out.toByteArray())
      .build();
  }
}
