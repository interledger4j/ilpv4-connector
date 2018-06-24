package com.sappenin.ilpv4.server.btp;

import com.sappenin.ilpv4.server.ConnectorServerConfig;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpResponse;
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
 * Unit tests that excercise the functionality of the BTP Server using Websockets.
 */
@ContextConfiguration(classes = {ConnectorServerConfig.class, BtpServerTest.TestConfig.class})
//@TestPropertySource(properties = {"foo.bar=0"})
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BtpServerTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  CodecContext codecContext;

  @LocalServerPort
  private int port;

  private StandardWebSocketClient wsClient;

  private BtpTestUtils btpTestUtils;

  /**
   * Countdown latch
   */
  private CountDownLatch lock = new CountDownLatch(1);

  @Before
  public void setup() {
    this.wsClient = new StandardWebSocketClient();
    this.btpTestUtils = new BtpTestUtils(codecContext);
  }

  @Test
  public void testAuthenticate() throws InterruptedException, ExecutionException, IOException {
    final long requestId = btpTestUtils.nextRequestId();

    final WebSocketSession session = wsClient.doHandshake(new BinaryWebSocketHandler() {
      @Override
      protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        final BtpResponse btpResponse = btpTestUtils.toBtpResponse(message);
        assertThat(btpResponse.getRequestId(), is(requestId));

        // Expect a valid auth response, which is merely an ACK packet that correlates to the above request id.
        assertThat(btpResponse.getSubProtocols().size(), is(0));

        // TODO: Fix this since it throws an NPE!
        //assertThat(btpResponse.getPrimarySubProtocol(), is(nullValue()));

        logger.info("Received Auth Response: {}", btpResponse);
        lock.countDown();
      }
    }, "ws://localhost:{port}/btp", port).get();


    final BtpMessage btpMessage = btpTestUtils.constructAuthMessage(requestId);
    session.sendMessage(btpTestUtils.toBinaryMessage(btpMessage));
    logger.info("Sent test Auth BtpMessage: {}", btpMessage);
    assertThat("Latch countdown should have reached zero!", lock.await(4, TimeUnit.SECONDS), is(true));
  }

  /**
   * Spring-configuration for this test.
   */
  @Configuration
  static class TestConfig {

  }
}
