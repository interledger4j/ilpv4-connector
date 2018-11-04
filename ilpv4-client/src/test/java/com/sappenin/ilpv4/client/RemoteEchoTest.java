package com.sappenin.ilpv4.client;

import com.sappenin.ilpv4.client.echo.EchoPacketService;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.core.*;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.BtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.plugin.lpiv2.btp2.subprotocols.ilp.DefaultIlpBtpSubprotocolHandler;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test to validate the BtpClient can send <tt>echo</tt> protocol packets to a remote JS connector.
 */
public class RemoteEchoTest {

  private static final int TIMEOUT_SECOND = 200;

  private static final InterledgerAddress CLIENT = InterledgerAddress.of("test.client");
  private static final InterledgerAddress VALID_ILP_NODE = InterledgerAddress.of("test.parent");
  private static final InterledgerAddress INVALID_ILP_NODE = InterledgerAddress.of("test.notfound");
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final CodecContext ilpCodecContext = InterledgerCodecContextFactory.oer();

  private BtpWsClient aliceClient;

  private EchoPacketService echoPacketService;

  @Before
  public void setup() throws InterruptedException, ExecutionException, TimeoutException {

    this.echoPacketService = new EchoPacketService(ilpCodecContext);

    final BtpClientPluginSettings settings = BtpClientPluginSettings.builder()
      .localNodeAddress(CLIENT)
      .peerAccountAddress(VALID_ILP_NODE)
      .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
      .secret("shh")
      .remotePeerPort("6667")
      .remotePeerScheme("ws")
      .build();
    final ClientWebsocketBtpPlugin plugin = new ClientWebsocketBtpPlugin(settings);

    final DefaultIlpBtpSubprotocolHandler ilpBtpSubprotocolHandler =
      new DefaultIlpBtpSubprotocolHandler(ilpCodecContext);
    plugin.getBtpSubProtocolHandlerRegistry().putHandler(
      BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP,
      BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM,
      ilpBtpSubprotocolHandler
    );

     TODO: We want the ilpPluginDataHandler to actually be the registered datahandler, but this doesn't appear to
     be happening automatically....so need to fix this.


      TODO: Consider changing the Async contract to return a packet instead of throwing an exception (if we keep using CompleteableFuture.)
    Instead, look at RxJava maybe for the async, and use the current for the sync pattern?

    aliceClient = new BtpWsClient(plugin);
    aliceClient.connect().get(5, TimeUnit.SECONDS);
  }

  /**
   * Ping "test.parent" and expect a ping-response back, which is actually just an ILP Reject packet.
   */
  @Test
  public void testAlicePingsIlpNode() {
    final InterledgerPreparePacket echoPacket =
      this.echoPacketService.constructEchoRequestPacket(CLIENT, VALID_ILP_NODE);
    final CountDownLatch countDownLatch = new CountDownLatch(1);

    try {
      aliceClient.sendData(echoPacket)
        .whenComplete(((fulfillPacket, throwable) -> {
          countDownLatch.countDown();
          if (fulfillPacket == null) {
            logger.error(throwable.getMessage(), throwable);
            fail("Expected fulfillPacket, but was a Reject: " + throwable.getCause().getMessage());
          } else {
            // Expect the returned echo payment to contain the original condition...
            assertThat(fulfillPacket.getFulfillment().getCondition(), is(echoPacket.getExecutionCondition()));
          }
        }))
        .get(TIMEOUT_SECOND, TimeUnit.SECONDS);
      countDownLatch.await(TIMEOUT_SECOND, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      aliceClient.disconnect();
      fail(e.getMessage());

      //      final InterledgerRejectPacket rejectPacket =
      //        ((InterledgerProtocolException) e.getCause()).getInterledgerRejectPacket();
      //      assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
      //      assertThat(rejectPacket.getMessage(), is("no route found. source=up destination=test.client"));
      //      logger.info("Echo Response: {}", rejectPacket);
      //    } catch (Exception e) {
      //
      //      throw new RuntimeException(e.getMessage(), e);
      //    }
    }
  }

  /**
   * Ping "test.notfound" and expect nothing back.
   */
  @Test
  public void testAlicePingsNonExistentIlpNode() {

    final InterledgerPreparePacket echoPacket =
      this.echoPacketService.constructEchoRequestPacket(CLIENT, INVALID_ILP_NODE);
    final CountDownLatch countDownLatch = new CountDownLatch(1);

    try {
      aliceClient.sendData(echoPacket).whenComplete(((fulfillPacket, throwable) -> countDownLatch.countDown()))
        .get(TIMEOUT_SECOND, TimeUnit.SECONDS);
      countDownLatch.await(TIMEOUT_SECOND, TimeUnit.SECONDS);
      fail();
    } catch (ExecutionException e) {
      final InterledgerRejectPacket rejectPacket =
        ((InterledgerProtocolException) e.getCause()).getInterledgerRejectPacket();
      assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F02_UNREACHABLE));
      assertThat(rejectPacket.getMessage(), is("no route found. source=up destination=test.notfound"));
      logger.info("Echo Response: {}", rejectPacket);
    } catch (Exception e) {
      aliceClient.disconnect();
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}
