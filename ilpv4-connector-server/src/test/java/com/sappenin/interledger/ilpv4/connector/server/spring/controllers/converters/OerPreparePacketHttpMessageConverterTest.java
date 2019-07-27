package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import com.google.common.collect.ImmutableList;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OerPreparePacketHttpMessageConverter}.
 */
@RunWith(Parameterized.class)
public class OerPreparePacketHttpMessageConverterTest {

  private static final InterledgerFulfillment FULFILLMENT = InterledgerFulfillment.of(new byte[32]);

  private static InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
    .executionCondition(FULFILLMENT.getCondition())
    .expiresAt(Instant.now())
    .destination(InterledgerAddress.of("example.receiver"))
    .amount(BigInteger.TEN)
    .data(new byte[32])
    .build();
  private static InterledgerFulfillPacket FULFILL_PACKET = InterledgerFulfillPacket.builder()
    .fulfillment(FULFILLMENT)
    .data(new byte[32])
    .build();
  private static InterledgerRejectPacket REJECT_PACKET = InterledgerRejectPacket.builder()
    .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
    .triggeredBy(InterledgerAddress.of("example.rejector"))
    .message("the message")
    .data(new byte[64])
    .build();

  private InterledgerPacket actualPacket;
  private InterledgerPacket expectedPacket;

  private OerPreparePacketHttpMessageConverter converter;

  public OerPreparePacketHttpMessageConverterTest(
    InterledgerPacket actualPacket,
    InterledgerPacket expectedPacket
  ) {
    this.actualPacket = actualPacket;
    this.expectedPacket = expectedPacket;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> packets() {
    return ImmutableList.of(
      // T Family
      new Object[]{PREPARE_PACKET, PREPARE_PACKET},
      new Object[]{FULFILL_PACKET, FULFILL_PACKET},
      new Object[]{REJECT_PACKET, REJECT_PACKET}
    );
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.converter = new OerPreparePacketHttpMessageConverter(InterledgerCodecContextFactory.oer());
  }

  @Test
  public void writeReadInternalPreparePacket() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    when(outputMessage.getBody()).thenReturn(baos);
    converter.writeInternal(actualPacket, null, outputMessage);

    HttpInputMessage inputMessageMock = mock(HttpInputMessage.class);
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    when(inputMessageMock.getBody()).thenReturn(bais);

    // Test readInternal
    InterledgerPacket actualPreparePacket =
      converter.readInternal(InterledgerPreparePacket.class, inputMessageMock);
    assertThat(actualPreparePacket, is(expectedPacket));
  }

  @Test
  public void writeReadPreparePacket() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    when(outputMessage.getBody()).thenReturn(baos);
    converter.writeInternal(actualPacket, null, outputMessage);

    HttpInputMessage inputMessageMock = mock(HttpInputMessage.class);
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    when(inputMessageMock.getBody()).thenReturn(bais);

    // Test read
    final InterledgerPacket actualPreparePacket = converter.read(null, null, inputMessageMock);
    assertThat(actualPreparePacket, is(expectedPacket));
  }

}
