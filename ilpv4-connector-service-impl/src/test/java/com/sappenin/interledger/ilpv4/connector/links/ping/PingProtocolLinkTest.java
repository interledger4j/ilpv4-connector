package com.sappenin.interledger.ilpv4.connector.links.ping;

import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PingProtocolLink}.
 */
public class PingProtocolLinkTest {

  private static final InterledgerAddress TARGET_ADDRESS = InterledgerAddress.of("example.target");
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Mock
  private LinkSettings linkSettings;

  private PingProtocolLink pingProtocolLink;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(linkSettings.getOperatorAddress()).thenReturn(TARGET_ADDRESS);
    this.pingProtocolLink = new PingProtocolLink(linkSettings, InterledgerCodecContextFactory.oer());
  }

  @Test
  public void testFulfillmentFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc=");
    final InterledgerFulfillment expectedFulfillment = InterledgerFulfillment.of(bytes);

    assertThat(expectedFulfillment, is(PingProtocolLink.PING_PROTOCOL_FULFILLMENT));
    assertThat(expectedFulfillment.getCondition(), is(PingProtocolLink.PING_PROTOCOL_CONDITION));
    assertThat(expectedFulfillment.validateCondition(PingProtocolLink.PING_PROTOCOL_CONDITION), is(true));
  }

  @Test
  public void testFulfillmentFromAscii() throws UnsupportedEncodingException {
    final InterledgerFulfillment expectedFulfillment =
      InterledgerFulfillment.of("pingpingpingpingpingpingpingping".getBytes("US-ASCII"));

    assertThat(expectedFulfillment, is(PingProtocolLink.PING_PROTOCOL_FULFILLMENT));
    assertThat(expectedFulfillment.getCondition(), is(PingProtocolLink.PING_PROTOCOL_CONDITION));
    assertThat(expectedFulfillment.validateCondition(PingProtocolLink.PING_PROTOCOL_CONDITION), is(true));
  }

  @Test
  public void testConditionFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34=");
    final InterledgerCondition expectedCondition = InterledgerCondition.of(bytes);

    assertThat(expectedCondition, is(PingProtocolLink.PING_PROTOCOL_CONDITION));
    assertThat(PingProtocolLink.PING_PROTOCOL_FULFILLMENT.getCondition(), is(PingProtocolLink.PING_PROTOCOL_CONDITION));
    assertThat(PingProtocolLink.PING_PROTOCOL_FULFILLMENT.validateCondition(PingProtocolLink.PING_PROTOCOL_CONDITION),
      is(true));
  }

  @Test
  public void sendPacketWithInvalidCondition() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .amount(BigInteger.TEN)
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .destination(InterledgerAddress.of("example.foo"))
      .expiresAt(Instant.now())
      .build();
    InterledgerResponsePacket response = pingProtocolLink.sendPacket(preparePacket);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        logger.error("InterledgerFulfillPacket: {}", interledgerFulfillPacket);
        fail("Expected a Reject!");
      }

      @Override
      protected void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
        assertThat(interledgerRejectPacket.getTriggeredBy(), is(TARGET_ADDRESS));
      }
    }.handle(response);
  }

  @Test
  public void sendPacket() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .amount(BigInteger.TEN)
      .executionCondition(PingProtocolLink.PING_PROTOCOL_CONDITION)
      .destination(InterledgerAddress.of("example.foo"))
      .expiresAt(Instant.now())
      .build();
    InterledgerResponsePacket response = pingProtocolLink.sendPacket(preparePacket);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(PingProtocolLink.PING_PROTOCOL_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        logger.error("InterledgerRejectPacket: {}", interledgerRejectPacket);
        fail("Expected a Fulfill!");
      }
    }.handle(response);
  }
}