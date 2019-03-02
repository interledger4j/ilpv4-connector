package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
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

/**
 * Unit tests for {@link PingProtocolFilter}.
 */
public class PingProtocolFilterTest {

  private static final InterledgerAddress TARGET_ADDRESS = InterledgerAddress.of("example.target");
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Mock
  PacketSwitchFilterChain filterChainMock;

  private PingProtocolFilter pingProtocolFilter;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.pingProtocolFilter = new PingProtocolFilter(() -> TARGET_ADDRESS);
  }

  @Test
  public void testFulfillmentFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc=");
    final InterledgerFulfillment expectedFulfillment = InterledgerFulfillment.of(bytes);

    assertThat(expectedFulfillment, is(pingProtocolFilter.PING_PROTOCOL_FULFILLMENT));
    assertThat(expectedFulfillment.getCondition(), is(pingProtocolFilter.PING_PROTOCOL_CONDITION));
    assertThat(expectedFulfillment.validateCondition(pingProtocolFilter.PING_PROTOCOL_CONDITION), is(true));
  }

  @Test
  public void testFulfillmentFromAscii() throws UnsupportedEncodingException {
    final InterledgerFulfillment expectedFulfillment =
      InterledgerFulfillment.of("pingpingpingpingpingpingpingping".getBytes("US-ASCII"));

    assertThat(expectedFulfillment, is(pingProtocolFilter.PING_PROTOCOL_FULFILLMENT));
    assertThat(expectedFulfillment.getCondition(), is(pingProtocolFilter.PING_PROTOCOL_CONDITION));
    assertThat(expectedFulfillment.validateCondition(pingProtocolFilter.PING_PROTOCOL_CONDITION), is(true));
  }

  @Test
  public void testConditionFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34=");
    final InterledgerCondition expectedCondition = InterledgerCondition.of(bytes);

    assertThat(expectedCondition, is(pingProtocolFilter.PING_PROTOCOL_CONDITION));
    assertThat(pingProtocolFilter.PING_PROTOCOL_FULFILLMENT.getCondition(),
      is(pingProtocolFilter.PING_PROTOCOL_CONDITION));
    assertThat(
      pingProtocolFilter.PING_PROTOCOL_FULFILLMENT.validateCondition(pingProtocolFilter.PING_PROTOCOL_CONDITION),
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
    InterledgerResponsePacket response = pingProtocolFilter.doFilter(
      AccountId.of("alice"), preparePacket, filterChainMock
    );

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
      .executionCondition(pingProtocolFilter.PING_PROTOCOL_CONDITION)
      .destination(InterledgerAddress.of("example.foo"))
      .expiresAt(Instant.now())
      .build();
    InterledgerResponsePacket response = pingProtocolFilter.doFilter(
      AccountId.of("alice"), preparePacket, filterChainMock
    );

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(pingProtocolFilter.PING_PROTOCOL_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        logger.error("InterledgerRejectPacket: {}", interledgerRejectPacket);
        fail("Expected a Fulfill!");
      }
    }.handle(response);
  }
}