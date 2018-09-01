package com.sappenin.ilpv4.connector.ccp.codecs;

import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.connector.ccp.*;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;

/**
 * Unit tests that test the encoding/decoding of a {@link UUID} to/from ASN.1 OER.
 */
public abstract class AbstractAsnCodecTest<T> {

  protected static CodecContext codecContext;

  static {
    // Register the codec to be tested...
    codecContext = InterledgerCodecContextFactory.oer();
    codecContext.register(UUID.class, AsnUuidCodec::new);
    codecContext.register(CcpRouteControlRequest.class, AsnCcpRouteControlRequestCodec::new);
    codecContext.register(CcpRouteControlResponse.class, AsnCcpRouteControlResponseCodec::new);
    codecContext.register(CcpRouteUpdateRequest.class, AsnCcpRouteUpdateRequestCodec::new);
    codecContext.register(CcpRoutePathPart.class, AsnCcpRoutePathPartCodec::new);
    codecContext.register(CcpRouteProperty.class, AsnCcpRoutePropertyCodec::new);
  }

  private final T expectedObject;
  private final byte[] asn1OerBytes;
  private final Class<T> clazz;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedObject
   * @param asn1OerBytes   The expected value, in binary, of the supplied {@code expectedPayloadLength}.
   * @param clazz          A {@link Class} corresponding to the type <T> for type-inference purposes.
   */
  public AbstractAsnCodecTest(
    final T expectedObject, final byte[] asn1OerBytes, final Class<T> clazz) {
    this.expectedObject = Objects.requireNonNull(expectedObject);
    this.asn1OerBytes = Objects.requireNonNull(asn1OerBytes);
    this.clazz = Objects.requireNonNull(clazz);
  }

  @Test
  public void read() throws IOException {
    // This getAllAccountSettings allows the codec to read the asn1Bytes...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(this.asn1OerBytes);
    final T actual = codecContext.read(clazz, inputStream);
    Assert.assertThat(actual, is(expectedObject));
  }

  @Test
  public void write() throws Exception {
    // Allow the AsnObjectCodec to write to 'outputStream'
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(expectedObject, outputStream);

    final byte[] actualBytes = outputStream.toByteArray();

    final String expectedHex = BaseEncoding.base16().encode(this.asn1OerBytes);
    final String actualHex = BaseEncoding.base16().encode(actualBytes);
    assertThat(actualHex, is(expectedHex));
    assertArrayEquals(this.asn1OerBytes, actualBytes);
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write octets...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(expectedObject, outputStream);

    // Read octets...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final T actual = codecContext.read(clazz, inputStream);
    Assert.assertThat(actual, is(expectedObject));

    // Write octets again...
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    codecContext.write(expectedObject, outputStream2);

    // Assert originally written bytes equals newly written bytes.
    Assert.assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }
}