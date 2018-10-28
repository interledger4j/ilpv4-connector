package com.sappenin.ilpv4.plugins.btp.spring.converters;

import org.interledger.btp.BtpPacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.socket.BinaryMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class BtpPacketToBinaryMessageConverter implements Converter<BtpPacket, BinaryMessage> {

  private final CodecContext codecContext;

  public BtpPacketToBinaryMessageConverter(final CodecContext codecContext) {
    this.codecContext = Objects.requireNonNull(codecContext);
  }

  @Override
  public BinaryMessage convert(final BtpPacket btpPacket) {
    Objects.requireNonNull(btpPacket, "btpPacket must not be null!");

    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      this.codecContext.write(btpPacket, baos);
      return new BinaryMessage(baos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
