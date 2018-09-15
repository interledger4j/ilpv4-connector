package com.sappenin.ilpv4.server.btp;

import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.plugins.btp.converters.BinaryMessageToBtpErrorConverter;
import com.sappenin.ilpv4.plugins.btp.converters.BinaryMessageToBtpResponseConverter;
import com.sappenin.ilpv4.plugins.btp.converters.BtpPacketToBinaryMessageConverter;
import com.sappenin.ilpv4.server.spring.SpringConnectorServerConfig;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpResponse;
import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests that exercise the ILP Prepare functionality of the BTP Server using Websockets.
 */
@ContextConfiguration(classes = {SpringConnectorServerConfig.class, BtpServerPrepareTest.TestConfig.class})
//@TestPropertySource(properties = {"foo.bar=0"})
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BtpServerPrepareTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  CodecContext codecContext;

  @Autowired
  BinaryMessageToBtpResponseConverter binaryMessageToBtpResponseConverter;

  @Autowired
  BinaryMessageToBtpErrorConverter binaryMessageToBtpErrorConverter;

  @Autowired
  BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

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
    this.btpTestUtils = new BtpTestUtils(codecContext);
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
        final BtpResponse btpResponse = binaryMessageToBtpResponseConverter.convert(message);
        assertThat(btpResponse.getRequestId(), is(requestId));

        // Expect a valid auth response, which is merely an ACK packet that correlates to the above request id.
        assertThat(btpResponse.getSubProtocols().size(), is(0));

        logger.info("Received Auth Response: {}", btpResponse);
        lock.countDown();
      }
    }, "ws://localhost:{port}/btp", port).get();


    // AUTH
    final BtpMessage btpAuthMessage = btpTestUtils.constructAuthMessage(requestId);
    final BinaryMessage binaryAuthMessage = btpPacketToBinaryMessageConverter.convert(btpAuthMessage);
    logger.info(
      "Websocket Auth BinaryMessage Bytes: {}", BaseEncoding.base16().encode(binaryAuthMessage.getPayload().array())
    );
    session.sendMessage(binaryAuthMessage);

    // PREPARE
    final BtpMessage btpPrepareMessage = btpTestUtils.constructIlpPrepareMessage(
      requestId, "foo", "bar", InterledgerAddress.of("test.parent.unlimited.usd")
    );
    //logger.info("Btp Prepare Message: {}", btpPrepareMessage);
    final BinaryMessage binaryMessage = btpPacketToBinaryMessageConverter.convert(btpPrepareMessage);
    logger.info(
      "Websocket Prepare BinaryMessage Bytes: {}: {}", BaseEncoding.base16().encode(binaryMessage.getPayload().array())
    );
    session.sendMessage(binaryMessage);
    assertThat("Latch countdown should have reached zero!", lock.await(5, TimeUnit.SECONDS), is(true));
  }

  /**
   * Spring-configuration for this test.
   */
  @Configuration
  static class TestConfig {

  }
}
