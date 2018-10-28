package com.sappenin.ilpv4.plugins.btp.spring.converters;

import org.interledger.btp.BtpResponse;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.socket.BinaryMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @deprecated Will go away in-favor of {@link BinaryMessageToBtpPacketConverter}.
 */
@Deprecated
public class BinaryMessageToBtpResponseConverter implements Converter<BinaryMessage, BtpResponse> {

  private final CodecContext codecContext;

  public BinaryMessageToBtpResponseConverter(final CodecContext codecContext) {
    this.codecContext = Objects.requireNonNull(codecContext);
  }

  @Override
  public BtpResponse convert(final BinaryMessage binaryMessage) {
    Objects.requireNonNull(binaryMessage, "binaryMessage must not be null!");

    try {
      final ByteBuffer buffer = binaryMessage.getPayload();
      final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
      return codecContext.read(BtpResponse.class, stream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
