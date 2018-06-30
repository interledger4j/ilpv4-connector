package com.sappenin.ilpv4.server.btp.converters;

import org.interledger.btp.BtpError;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.socket.BinaryMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class BinaryMessageToBtpErrorConverter implements Converter<BinaryMessage, BtpError> {

  private final CodecContext codecContext;

  public BinaryMessageToBtpErrorConverter(final CodecContext codecContext) {
    this.codecContext = Objects.requireNonNull(codecContext);
  }

  @Override
  public BtpError convert(final BinaryMessage binaryMessage) {
    Objects.requireNonNull(binaryMessage, "binaryMessage must not be null!");

    try {
      final ByteBuffer buffer = binaryMessage.getPayload();
      final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
      return codecContext.read(BtpError.class, stream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
