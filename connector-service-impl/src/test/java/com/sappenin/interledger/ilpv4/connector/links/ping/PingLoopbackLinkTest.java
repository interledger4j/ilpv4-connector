package com.sappenin.interledger.ilpv4.connector.links.ping;

import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.events.LinkEventEmitter;
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.sappenin.interledger.ilpv4.connector.links.ping.PingLoopbackLink.PING_PROTOCOL_CONDITION;
import static com.sappenin.interledger.ilpv4.connector.links.ping.PingLoopbackLink.PING_PROTOCOL_FULFILLMENT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link PingLoopbackLink}.
 */
public class PingLoopbackLinkTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.foo");

  private static final InterledgerRejectPacket EXPECTED_REJECT_PACKET = InterledgerRejectPacket.builder()
    .triggeredBy(OPERATOR_ADDRESS)
    .code(InterledgerErrorCode.F00_BAD_REQUEST)
    .message("error message")
    .build();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Mock
  LinkSettings linkSettingsMock;

  @Mock
  LinkEventEmitter linkEventEmitterMock;

  private PingLoopbackLink link;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.link = new PingLoopbackLink(() -> Optional.of(OPERATOR_ADDRESS), linkSettingsMock, linkEventEmitterMock);
  }

  @Test
  public void doConnect() throws ExecutionException, InterruptedException {
    assertThat(link.doConnect().get(), is(nullValue()));
  }

  @Test
  public void doDisconnect() throws ExecutionException, InterruptedException {
    assertThat(link.doDisconnect().get(), is(nullValue()));
  }

  @Test(expected = RuntimeException.class)
  public void registerLinkHandler() {
    try {
      link.registerLinkHandler(incomingPreparePacket -> null);
    } catch (Exception e) {
      assertThat(e.getMessage(), is(
        "Ping links never have incoming data from a remote Connector, and thus should not have a registered DataHandler."));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void sendPacketWithNull() {
    try {
      link.sendPacket(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("preparePacket must not be null!"));
      throw e;
    }
  }

  @Test
  public void testFulfillmentFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc=");
    final InterledgerFulfillment expectedFulfillment = InterledgerFulfillment.of(bytes);

    assertThat(expectedFulfillment, is(PING_PROTOCOL_FULFILLMENT));
    assertThat(expectedFulfillment.getCondition(), is(PING_PROTOCOL_CONDITION));
    assertThat(expectedFulfillment.validateCondition(PING_PROTOCOL_CONDITION), is(true));
  }

  @Test
  public void testFulfillmentFromAscii() throws UnsupportedEncodingException {
    final InterledgerFulfillment expectedFulfillment =
      InterledgerFulfillment.of("pingpingpingpingpingpingpingping".getBytes("US-ASCII"));

    assertThat(expectedFulfillment, is(PING_PROTOCOL_FULFILLMENT));
    assertThat(expectedFulfillment.getCondition(), is(PING_PROTOCOL_CONDITION));
    assertThat(expectedFulfillment.validateCondition(PING_PROTOCOL_CONDITION), is(true));
  }

  @Test
  public void testConditionFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34=");
    final InterledgerCondition expectedCondition = InterledgerCondition.of(bytes);

    assertThat(expectedCondition, is(PING_PROTOCOL_CONDITION));
    assertThat(PING_PROTOCOL_FULFILLMENT.getCondition(), is(PING_PROTOCOL_CONDITION));
    assertThat(PING_PROTOCOL_FULFILLMENT.validateCondition(PING_PROTOCOL_CONDITION), is(true));
  }

  @Test
  public void sendPacketWithInvalidCondition() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .amount(BigInteger.TEN)
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .destination(OPERATOR_ADDRESS)
      .expiresAt(Instant.now())
      .build();

    final InterledgerResponsePacket response = link.sendPacket(preparePacket);
    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        logger.error("InterledgerFulfillPacket: {}", interledgerFulfillPacket);
        fail("Expected a Reject!");
      }

      @Override
      protected void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
        assertThat(interledgerRejectPacket.getTriggeredBy().isPresent(), is(true));
        assertThat(interledgerRejectPacket.getTriggeredBy(), is(EXPECTED_REJECT_PACKET.getTriggeredBy()));
      }
    }.handle(response);
  }

  /**
   * A packet with an invalid destination address should never make its way into this link, so this tests expects a
   * Fulfill even though that's counter-intuitive -- this is because the Link itself does not destination address
   * checking.
   */
  @Test
  public void sendPacketWithInvalidAddress() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .amount(BigInteger.TEN)
      .executionCondition(PING_PROTOCOL_CONDITION)
      .destination(InterledgerAddress.of("example.foo"))
      .expiresAt(Instant.now())
      .build();
    final InterledgerResponsePacket response = link.sendPacket(preparePacket);
    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(PING_PROTOCOL_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        logger.error("InterledgerRejectPacket: {}", interledgerRejectPacket);
        fail("Expected a Fulfill!");
      }
    }.handle(response);
  }

  @Test
  public void sendPacket() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .amount(BigInteger.TEN)
      .executionCondition(PING_PROTOCOL_CONDITION)
      .destination(OPERATOR_ADDRESS)
      .expiresAt(Instant.now())
      .build();

    final InterledgerResponsePacket response = link.sendPacket(preparePacket);
    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(PING_PROTOCOL_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        logger.error("InterledgerRejectPacket: {}", interledgerRejectPacket);
        fail("Expected a Fulfill!");
      }
    }.handle(response);

    //verifyZeroInteractions(filterChainMock);
  }

}
