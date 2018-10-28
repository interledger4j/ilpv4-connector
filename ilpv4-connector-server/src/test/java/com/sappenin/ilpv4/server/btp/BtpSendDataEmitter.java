package com.sappenin.ilpv4.server.btp;

import com.google.common.io.BaseEncoding;
import org.interledger.btp.*;
import org.interledger.btp.asn.framework.BtpCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH;
import static org.interledger.btp.BtpSubProtocols.INTERLEDGER;

/**
 * Helper class to assembles a binary message for BTP for testing in a web-browser, for example.
 */
public class BtpSendDataEmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(BtpSendDataEmitter.class);

  //private static final CodecContext ILP_CONTEXT =
  // CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES);
  private static final CodecContext BTP_CONTEXT = BtpCodecContextFactory.oer();

  private static final long REQUEST_ID = 1;

  public static void main(String[] args) throws IOException {
    emitBtpTransferBytes();
    emitBtpAuthMessageBytes();
  }

  private static void emitBtpTransferBytes() throws IOException {
    final BtpTransfer transfer = BtpTransfer.builder()
      .requestId(REQUEST_ID)
      .amount(BigInteger.TEN)
      .subProtocols(new BtpSubProtocols())
      .build();

    final ByteArrayOutputStream transferOs = new ByteArrayOutputStream();
    BTP_CONTEXT.write(transfer, transferOs);
    final BtpSubProtocol ilpSubProtocol = BtpSubProtocol.builder()
      .protocolName(INTERLEDGER)
      .contentType(BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM)
      .data(transferOs.toByteArray())
      .build();

    BtpSubProtocols btpSubProtocols = new BtpSubProtocols();
    btpSubProtocols.add(ilpSubProtocol);

    final BtpMessage transferMessage = BtpMessage.builder()
      .requestId(REQUEST_ID)
      .subProtocols(btpSubProtocols)
      .build();

    emitBtpPacket(transferMessage);
  }

  private static void emitBtpAuthMessageBytes() {

    final BtpSubProtocol authSubProtocol = BtpSubProtocol.builder()
      .protocolName(BTP_SUB_PROTOCOL_AUTH)
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocol authUsernameSubprotocol = BtpSubProtocol.builder()
      .protocolName("auth_username")
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("g.david.guin".getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocol authTokenSubprotocol = BtpSubProtocol.builder()
      .protocolName("auth_token")
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("password".getBytes(StandardCharsets.UTF_8))
      .build();

    final BtpSubProtocols btpSubProtocols = new BtpSubProtocols();
    btpSubProtocols.add(authSubProtocol);
    btpSubProtocols.add(authUsernameSubprotocol);
    btpSubProtocols.add(authTokenSubprotocol);

    final BtpMessage authMessage = BtpMessage.builder()
      .requestId(REQUEST_ID)
      .subProtocols(btpSubProtocols)
      .build();
    emitBtpPacket(authMessage);
  }

  private static final void emitBtpPacket(final BtpPacket btpPacket) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();

      BTP_CONTEXT.write(btpPacket, os);
      final String transferBase64 = BaseEncoding.base16().encode(os.toByteArray());
      LOGGER.info("{} Hex Bytes: {}", btpPacket.getClass().getSimpleName(), transferBase64);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

}
