package org.interledger.connector.server.spring.controllers.converters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.connector.core.Ilpv4Constants.ALL_ZEROS_FULFILLMENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.server.spring.controllers.IlpHttpController;
import org.interledger.connector.server.spring.controllers.settlement.SettlementController;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

/**
 * Unit tests for {@link OerPreparePacketHttpMessageConverter}.
 */
@RunWith(Parameterized.class)
public class OerPreparePacketHttpMessageConverterTest {

  private static InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
      .executionCondition(ALL_ZEROS_FULFILLMENT.getCondition())
      .expiresAt(Instant.now().truncatedTo(ChronoUnit.MILLIS))
      .destination(InterledgerAddress.of("example.receiver"))
      .amount(UnsignedLong.valueOf(10))
      .data(new byte[32])
      .build();
  private static InterledgerFulfillPacket FULFILL_PACKET = InterledgerFulfillPacket.builder()
      .fulfillment(ALL_ZEROS_FULFILLMENT)
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
        new Object[] {PREPARE_PACKET, PREPARE_PACKET},
        new Object[] {FULFILL_PACKET, FULFILL_PACKET},
        new Object[] {REJECT_PACKET, REJECT_PACKET}
    );
  }

  @Before
  public void setUp() {
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

  @Test
  public void testCanRead() {
    // Happy paths...
    assertThat(converter.canRead(InterledgerPreparePacket.class, IlpHttpController.class, APPLICATION_JSON),
        is(false));
    assertThat(converter.canRead(InterledgerPreparePacket.class, IlpHttpController.class, APPLICATION_OCTET_STREAM),
        is(true));

    // Wrong Controller...
    assertThat(converter.canRead(InterledgerPreparePacket.class, SettlementController.class, APPLICATION_JSON),
        is(false));
    assertThat(converter.canRead(InterledgerPreparePacket.class, SettlementController.class, APPLICATION_OCTET_STREAM),
        is(false));

    // Wrong payload...
    assertThat(converter.canRead(String.class, IlpHttpController.class, APPLICATION_JSON), is(false));
    assertThat(converter.canRead(String.class, IlpHttpController.class, APPLICATION_OCTET_STREAM), is(true));

    // Null combos
    assertThat(converter.canRead(null, IlpHttpController.class, APPLICATION_OCTET_STREAM), is(true));
    assertThat(converter.canRead(InterledgerPreparePacket.class, null, APPLICATION_OCTET_STREAM), is(false));
    assertThat(converter.canRead(InterledgerPreparePacket.class, IlpHttpController.class, null), is(true));
    assertThat(converter.canRead(null, null, null), is(false));
  }

}
