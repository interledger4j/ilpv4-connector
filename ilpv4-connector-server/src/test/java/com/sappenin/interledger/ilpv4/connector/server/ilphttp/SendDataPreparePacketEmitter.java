package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.google.common.io.BaseEncoding;
import org.apache.commons.io.FileUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
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
 * Helper class to assembles a binary message for BTP for testing in a web-browser, for example.
 */
public class SendDataPreparePacketEmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SendDataPreparePacketEmitter.class);

  private static final CodecContext ILP_CONTEXT = InterledgerCodecContextFactory.oer();

  public static void main(String[] args) {
    emitPreparePacketBytes();
  }

  private static void emitPreparePacketBytes() {
    final InterledgerCondition executionCondition = InterledgerCondition.of(new byte[32]);
    final InterledgerPreparePacket.AbstractInterledgerPreparePacket packet = InterledgerPreparePacket.builder()
      .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
      .destination(InterledgerAddress.of("test.foo"))
      .amount(BigInteger.TEN)
      .executionCondition(executionCondition)
      .build();

    emitIlpPacketToFile(packet);
  }

  private static final void emitIlpPacket(final InterledgerPacket interledgerPacket) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();

      ILP_CONTEXT.write(interledgerPacket, os);
      final String transferBase64 = BaseEncoding.base16().encode(os.toByteArray());
      LOGGER.info("{} Hex Bytes: {}", interledgerPacket.getClass().getSimpleName(), transferBase64);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private static final void emitIlpPacketToFile(final InterledgerPacket interledgerPacket) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();

      ILP_CONTEXT.write(interledgerPacket, os);
      FileUtils.writeByteArrayToFile(new File("/tmp/testPreparePacket.bin"), os.toByteArray());

      final String transferBase64 = BaseEncoding.base16().encode(os.toByteArray());
      LOGGER.info("{} Hex Bytes: {}", interledgerPacket.getClass().getSimpleName(), transferBase64);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }


  }

}
