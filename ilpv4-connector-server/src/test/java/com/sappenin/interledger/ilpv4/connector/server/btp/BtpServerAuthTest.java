package com.sappenin.interledger.ilpv4.connector.server.btp;

import com.sappenin.interledger.ilpv4.connector.ImmutableAccount;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.SpringConnectorWebMvc;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableAccountSettings;
import org.interledger.btp.BtpError;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpPacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.spring.BtpServerPlugin;
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
import static com.sappenin.interledger.ilpv4.connector.server.btp.BtpTestUtils.LOCAL_ACCOUNT_ID;
import static com.sappenin.interledger.ilpv4.connector.server.btp.BtpTestUtils.TEST_AUTH_USERNAME;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.BTP;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH_TOKEN;

/**
 * Unit tests that exercise the functionality of the BTP Server using WebSockets.
 */
@ContextConfiguration(classes = {SpringConnectorWebMvc.class, BtpServerAuthTest.TestConfig.class})
//@TestPropertySource(properties = {"foo.bar=0"})
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BtpServerAuthTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  @Qualifier(BTP)
  CodecContext btpCodecContext;

  @Autowired
  @Qualifier(ILP)
  CodecContext ilpCodecContext;

  @Autowired
  com.sappenin.interledger.ilpv4.connector.ILPv4Connector ILPv4Connector;

  @Autowired
  AccountManager accountManager;

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
  private CountDownLatch lock = new CountDownLatch(1);

  @Before
  public void setup() {
    this.wsClient = new StandardWebSocketClient();
    this.btpTestUtils = new BtpTestUtils(ilpCodecContext, btpCodecContext);

    // Only add the account to the test-connector instance once per test-run.
    if (accountManager.getAccount(LOCAL_ACCOUNT_ID).isPresent() == false) {
      accountManager.addAccount(
        ImmutableAccount.builder()
          .accountSettings(
            ImmutableAccountSettings.builder()
              .id(LOCAL_ACCOUNT_ID)
              .pluginType(BtpServerPlugin.PLUGIN_TYPE)
              .build()
          )
          .build()
      );
    }
  }

  /**
   * Open a websocket connection and send a BTP Authenticate message.
   */
  @Test
  public void testAuthenticate() throws InterruptedException, ExecutionException, IOException {
    final long requestId = btpTestUtils.nextRequestId();

    final WebSocketSession session = wsClient.doHandshake(new BinaryWebSocketHandler() {
      @Override
      protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        final BtpPacket btpResponse = binaryMessageToBtpPacketConverter.convert(message);
        assertThat(btpResponse.getRequestId(), is(requestId));

        // Expect a valid auth response, which is merely an ACK packet that correlates to the above request id.
        assertThat(btpResponse.getSubProtocols().size(), is(0));

        logger.info("Received Auth Response: {}", btpResponse);
        lock.countDown();
      }
    }, "ws://localhost:{port}/btp", port).get();


    final BtpMessage btpMessage = btpTestUtils.constructAuthMessage(requestId);
    session.sendMessage(btpPacketToBinaryMessageConverter.convert(btpMessage));
    logger.info("Sent test Auth BtpMessage: {}", btpMessage);
    assertThat("Latch countdown should have reached zero!", lock.await(LATCH_LOCK_TIMEOUT, TimeUnit.SECONDS), is(true));
  }

  /**
   * Open a websocket connection and send a BTP Authenticate message that is missing its auth_token.
   */
  @Test
  public void testAuthenticateWithNoAuthToken() throws InterruptedException, ExecutionException, IOException {
    final long requestId = btpTestUtils.nextRequestId();

    final WebSocketSession session = wsClient.doHandshake(new BinaryWebSocketHandler() {
      @Override
      protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        final BtpError btpError = (BtpError) binaryMessageToBtpPacketConverter.convert(message);
        assertThat(btpError.getRequestId(), is(requestId));
        assertThat(btpError.getSubProtocols().size(), is(0));
        assertThat(new String(btpError.getErrorData()),
          is("Expected BTP SubProtocol with Id: " + BTP_SUB_PROTOCOL_AUTH_TOKEN));

        logger.info("Received Auth Error Respsonse: {}", btpError);
        lock.countDown();
      }
    }, "ws://localhost:{port}/btp", port).get();

    final BtpMessage btpMessage = btpTestUtils.constructAuthMessageWithNoAuthToken(requestId);
    session.sendMessage(btpPacketToBinaryMessageConverter.convert(btpMessage));
    logger.info("Sent test Auth BtpMessage: {}", btpMessage);
    assertThat("Latch countdown should have reached zero!", lock.await(LATCH_LOCK_TIMEOUT, TimeUnit.SECONDS), is(true));
  }

  /**
   * Open a websocket connection and send a BTP Authenticate message that is missing its auth_username.
   */
  @Test
  public void testAuthenticateWithNoAuthUsername() throws InterruptedException, ExecutionException, IOException {
    final long requestId = btpTestUtils.nextRequestId();

    final WebSocketSession session = wsClient.doHandshake(new BinaryWebSocketHandler() {
      @Override
      protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        final BtpPacket btpResponse = binaryMessageToBtpPacketConverter.convert(message);
        assertThat(btpResponse.getRequestId(), is(requestId));
        assertThat(btpResponse.getSubProtocols().size(), is(0));
        logger.info("Received Auth Response: {}", btpResponse);
        lock.countDown();
      }
    }, "ws://localhost:{port}/btp", port).get();

    final BtpMessage btpMessage = btpTestUtils.constructAuthMessageWithNoAuthUsername(requestId);
    session.sendMessage(btpPacketToBinaryMessageConverter.convert(btpMessage));
    logger.info("Sent test Auth BtpMessage: {}", btpMessage);
    assertThat("Latch countdown should have reached zero!", lock.await(LATCH_LOCK_TIMEOUT, TimeUnit.SECONDS), is(true));
  }

  /**
   * Open a websocket connection and send a BTP Authenticate message that is missing its auth_token.
   */
  @Test
  public void testAuthenticateWithInvalidAuthToken() throws InterruptedException, ExecutionException, IOException {
    final long requestId = btpTestUtils.nextRequestId();

    final WebSocketSession session = wsClient.doHandshake(new BinaryWebSocketHandler() {
      @Override
      protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        final BtpError btpError = (BtpError) binaryMessageToBtpPacketConverter.convert(message);
        assertThat(btpError.getRequestId(), is(requestId));
        assertThat(btpError.getSubProtocols().size(), is(0));
        assertThat(new String(btpError.getErrorData()), is("invalid auth_token"));

        logger.info("Received Auth Error Respsonse: {}", btpError);
        lock.countDown();
      }
    }, "ws://localhost:{port}/btp", port).get();

    final BtpMessage btpMessage = btpTestUtils.constructAuthMessage(requestId, TEST_AUTH_USERNAME, "");
    session.sendMessage(btpPacketToBinaryMessageConverter.convert(btpMessage));
    logger.info("Sent test Auth BtpMessage: {}", btpMessage);
    assertThat("Latch countdown should have reached zero!", lock.await(LATCH_LOCK_TIMEOUT, TimeUnit.SECONDS), is(true));
  }

  /**
   * Spring-configuration for this test.
   */
  @Configuration
  static class TestConfig {

  }
}
