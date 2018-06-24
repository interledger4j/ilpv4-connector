package com.sappenin.ilpv4.server.btp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.plugins.PluginManager;
import org.interledger.btp.*;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
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
  private final PluginManager pluginManager;

  public IlpSubprotocolHandler(final CodecContext codecContext, PluginManager pluginManager) {
    this.codecContext = Objects.requireNonNull(codecContext);
    this.pluginManager = Objects.requireNonNull(pluginManager);
  }

  @Override
  public byte[] handleBinaryMessage(BtpSession session, byte[] data) {
    Objects.requireNonNull(session);
    Objects.requireNonNull(data);

    if (logger.isDebugEnabled()) {
      logger.debug("Handling BTP data from {}: {}", BaseEncoding.base64().encode(data), session.getAccountId());
    }

    // Per RFC-23, ILP packets are attached under the protocol name "ilp" with content-type "application/octet-stream"
    try {
      final BtpMessage btpMessage = codecContext.read(BtpMessage.class, new ByteArrayInputStream(data));
      final BtpSubProtocol ilpSubProtocol = btpMessage.getSubProtocol(INTERLEDGER);

      // Convert to an ILP Prepare packet.
      final InterledgerPreparePacket incomingPreparePacket =
        codecContext.read(InterledgerPreparePacket.class, new ByteArrayInputStream(ilpSubProtocol.getData()));

      // Handle the packet, then convert to BTP response, and return.
      final Plugin plugin = this.pluginManager.getPlugin(session.getAccountId()).orElseThrow(
        // TODO: If this happens, will the catch block below handle this properly?
        () -> new RuntimeException("Not Plugin found for Account %s")
      );

      return plugin.onIncomingPacket
        (incomingPreparePacket)
        .thenApply(this::toBtpResponse)
        // TODO: Change this to be a configurable timeout!
        .get(30, TimeUnit.SECONDS);

    } catch (InterledgerProtocolException e) {
      // TODO: Return an appropriate BTP error message with an ILP Error message as data, per https://github
      // .com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md
      throw new RuntimeException(e);
    } catch (IOException | InterruptedException | ExecutionException | TimeoutException | RuntimeException e) {
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
