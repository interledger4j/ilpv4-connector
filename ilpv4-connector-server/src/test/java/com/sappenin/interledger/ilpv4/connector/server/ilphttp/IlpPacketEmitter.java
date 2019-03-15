package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.google.common.io.BaseEncoding;
import org.apache.commons.io.FileUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Helper class to assemble binary packets for manual testing purposes. All packets are written to files in the `/tmp`
 * directory.
 */
public class IlpPacketEmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(IlpPacketEmitter.class);

  private static final CodecContext ILP_CONTEXT = InterledgerCodecContextFactory.oer();

  public static void main(String[] args) {
    emitPreparePacketBytes();
  }

  private static void emitPreparePacketBytes() {
    final InterledgerCondition executionCondition = InterledgerCondition.of(new byte[32]);
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
      .destination(InterledgerAddress.of("test.foo"))
      .amount(BigInteger.TEN)
      .executionCondition(executionCondition)
      .build();

    emitPacketToFile("/tmp/testPreparePacket.bin", preparePacket);

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F02_UNREACHABLE)
      .triggeredBy(InterledgerAddress.of("test.foo"))
      .message("")
      .build();
    emitPacketToFile("/tmp/testRejectPacket.bin", rejectPacket);

    final InterledgerFulfillPacket fulfillPacket = InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();
    emitPacketToFile("/tmp/testFulfillPacket.bin", fulfillPacket);
  }

  private static final void emitPacketToFile(final String fileName, final InterledgerPacket interledgerPacket) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      ILP_CONTEXT.write(interledgerPacket, os);
      FileUtils.writeByteArrayToFile(new File(fileName), os.toByteArray());

      final String transferBase64 = BaseEncoding.base16().encode(os.toByteArray());
      LOGGER.info("{} Hex Bytes: {}", fileName, transferBase64);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

}
