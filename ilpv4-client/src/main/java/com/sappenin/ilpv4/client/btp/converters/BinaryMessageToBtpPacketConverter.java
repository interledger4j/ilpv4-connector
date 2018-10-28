package com.sappenin.ilpv4.client.btp.converters;

import org.interledger.btp.BtpPacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.socket.BinaryMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

// TODO: Remove dependencies on Spring in this module, if possible.
public class BinaryMessageToBtpPacketConverter implements Converter<BinaryMessage, BtpPacket> {

  private final CodecContext codecContext;

  public BinaryMessageToBtpPacketConverter(final CodecContext codecContext) {
    this.codecContext = Objects.requireNonNull(codecContext);
  }

  @Override
  public BtpPacket convert(final BinaryMessage binaryMessage) {
    Objects.requireNonNull(binaryMessage, "binaryMessage must not be null!");

    try {
      final ByteBuffer buffer = binaryMessage.getPayload();
      final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
      return codecContext.read(BtpPacket.class, stream);
    } catch (IOException e) {
      throw new BtpConversionException(e);
    }
  }
}
