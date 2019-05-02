package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

import static org.interledger.connector.link.blast.BlastHeaders.ILP_HEADER_OCTET_STREAM;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_OCTET_STREAM;

/**
 * An {@link HttpMessageConverter} that handles instances of {@link InterledgerPreparePacket}.
 */
public class OerPreparePacketHttpMessageConverter extends AbstractGenericHttpMessageConverter<InterledgerPacket>
  implements HttpMessageConverter<InterledgerPacket> {

  private final CodecContext ilpCodecContext;

  public OerPreparePacketHttpMessageConverter(final CodecContext ilpCodecContext) {
    super(MediaType.APPLICATION_OCTET_STREAM, ILP_OCTET_STREAM, ILP_HEADER_OCTET_STREAM);
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
  }

  /**
   * Abstract template method that writes the actual body. Invoked fromEncodedValue {@link #write}.
   *
   * @param interledgerPacket the object to write to the output message
   * @param type              the type of object to write (may be {@code null})
   * @param outputMessage     the HTTP output message to write to
   *
   * @throws IOException                     in case of I/O errors
   * @throws HttpMessageNotWritableException in case of conversion errors
   */
  @Override
  protected void writeInternal(
    final InterledgerPacket interledgerPacket,
    final Type type,
    final HttpOutputMessage outputMessage
  ) throws IOException, HttpMessageNotWritableException {
    ilpCodecContext.write(interledgerPacket, outputMessage.getBody());
  }

  /**
   * Abstract template method that reads the actual object. Invoked fromEncodedValue {@link #read}.
   *
   * @param clazz        the type of object to return
   * @param inputMessage the HTTP input message to read fromEncodedValue
   *
   * @return the converted object
   *
   * @throws IOException                     in case of I/O errors
   * @throws HttpMessageNotReadableException in case of conversion errors
   */
  @Override
  protected InterledgerPacket readInternal(Class<? extends InterledgerPacket> clazz, HttpInputMessage inputMessage)
    throws IOException, HttpMessageNotReadableException {

    // This line is necessary in order to fully consume the message body...
    final byte[] bytes = StreamUtils.copyToByteArray(inputMessage.getBody());
    final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    return ilpCodecContext.read(InterledgerPacket.class, is);
  }

  /**
   * Read an object of the given type form the given input message, and returns it.
   *
   * @param type         the (potentially generic) type of object to return. This type must have previously been passed
   *                     to the {@link #canRead canRead} method of this interface, which must have returned {@code
   *                     true}.
   * @param contextClass a context class for the target type, for example a class in which the target type appears in a
   *                     method signature (can be {@code null})
   * @param inputMessage the HTTP input message to read fromEncodedValue
   *
   * @return the converted object
   *
   * @throws IOException                     in case of I/O errors
   * @throws HttpMessageNotReadableException in case of conversion errors
   */
  @Override
  public InterledgerPacket read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
    throws IOException, HttpMessageNotReadableException {

    // This line is necessary in order to fully consume the message body...
    final byte[] bytes = StreamUtils.copyToByteArray(inputMessage.getBody());
    final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    return ilpCodecContext.read(InterledgerPacket.class, is);
  }
}
