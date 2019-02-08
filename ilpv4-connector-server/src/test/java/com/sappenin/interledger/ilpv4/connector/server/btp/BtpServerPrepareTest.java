package com.sappenin.interledger.ilpv4.connector.server.btp;

import com.google.common.io.BaseEncoding;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.SpringConnectorWebMvc;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpMessageType;
import org.interledger.btp.BtpPacket;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.sappenin.interledger.ilpv4.connector.server.btp.BtpTestUtils.LATCH_LOCK_TIMEOUT;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.BTP;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests that exercise the ILP Prepare functionality of the BTP Server using Websockets.
 */
@ContextConfiguration(classes = {SpringConnectorWebMvc.class, BtpServerPrepareTest.TestConfig.class})
//@TestPropertySource(properties = {"foo.bar=0"})
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BtpServerPrepareTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  @Qualifier(BTP)
  CodecContext btpCodecContext;

  @Autowired
  @Qualifier(ILP)
  CodecContext ilpCodecContext;

  @Autowired
  BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  @Autowired
  BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter;

  @LocalServerPort
  private int port;

  private StandardWebSocketClient wsClient;

  private BtpTestUtils btpTestUtils;

  /**
   * Countdown latch
   */
  private CountDownLatch lock = new CountDownLatch(2);

  @Before
  public void setup() {
    this.wsClient = new StandardWebSocketClient();
    this.btpTestUtils = new BtpTestUtils(ilpCodecContext, btpCodecContext);
  }

  /**
   * Open a websocket connection, authenticate, and then send a BTP message with an ILP Prepare payload.
   */
  @Test
  public void testIlpPrepare() throws InterruptedException, ExecutionException, IOException {
    final long requestId = btpTestUtils.nextRequestId();

    final WebSocketSession session = wsClient.doHandshake(new BinaryWebSocketHandler() {
      @Override
      protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        final BtpPacket btpResponse = binaryMessageToBtpPacketConverter.convert(message);
        assertThat(btpResponse.getRequestId(), is(requestId));

        if (btpResponse.hasSubProtocol(BtpSubProtocols.INTERLEDGER)) {
          // Expect a valid auth response, which is merely an ACK packet that correlates to the above request id.
          logger.info("Received IlpFulfill Response: {}", btpResponse);
          assertThat(btpResponse.getType(), is(BtpMessageType.RESPONSE));
          assertThat(btpResponse.getSubProtocols().size(), is(1));
        } else {
          // Expect a valid auth response, which is merely an ACK packet that correlates to the above request id.
          logger.info("Received Auth Response: {}", btpResponse);
          assertThat(btpResponse.getType(), is(BtpMessageType.RESPONSE));
          assertThat(btpResponse.getSubProtocols().size(), is(0));
        }

        lock.countDown();
      }
    }, "ws://localhost:{port}/btp", port).get();


    // AUTH
    final BtpMessage btpAuthMessage = btpTestUtils.constructAuthMessage(requestId);
    final BinaryMessage binaryAuthMessage = btpPacketToBinaryMessageConverter.convert(btpAuthMessage);
    logger.info("Sending binaryAuthMessage: {}", BaseEncoding.base16().encode(binaryAuthMessage.getPayload().array()));
    session.sendMessage(binaryAuthMessage);

    // PREPARE
    final BtpMessage btpPrepareMessage = btpTestUtils.constructIlpPrepareMessage(
      requestId, InterledgerAddress.of("test.parent.unlimited.usd")
    );
    final BinaryMessage binaryMessage = btpPacketToBinaryMessageConverter.convert(btpPrepareMessage);
    logger
      .info("Sending  IlpPrepare BinaryMessage: {}", BaseEncoding.base16().encode(binaryMessage.getPayload().array()));
    session.sendMessage(binaryMessage);
    assertThat("Latch countdown should have reached zero!", lock.await(LATCH_LOCK_TIMEOUT, TimeUnit.SECONDS), is(true));
  }

  /**
   * Spring-configuration for this test.
   */
  @Configuration
  static class TestConfig {

  }
}
