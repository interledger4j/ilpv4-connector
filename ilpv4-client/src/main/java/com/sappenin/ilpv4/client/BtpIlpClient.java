package com.sappenin.ilpv4.client;

import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.client.btp.LoggingIlpBtpSubprotocolHandler;
import com.sappenin.ilpv4.client.btp.converters.BinaryMessageToBtpPacketConverter;
import com.sappenin.ilpv4.client.btp.converters.BtpConversionException;
import com.sappenin.ilpv4.client.btp.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.btp.*;
import org.interledger.btp.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.btp.subprotocols.ilp.IlpBtpSubprotocolHandler;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.PluginType;
import org.interledger.plugin.lpiv2.btp.AbstractBtpPlugin;
import org.interledger.plugin.lpiv2.btp.BtpClientPluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * An implementation of {@link IlpClient} that connects to a remote peer using BTP, but merely logs all responses.
 *
 * // TODO: This should probably be the actual BTP client. Users that want to use this class should register
 * handlers...
 */
public class BtpIlpClient extends AbstractBtpPlugin<BtpClientPluginSettings> implements
  IlpClient<BtpClientPluginSettings> {

  private static final String PLUGIN_TYPE_STRING = "BtpIlpClient";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);

  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  private final CodecContext ilpCodecContext;
  //private final CodecContext btpCodecContext;

  private final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter;
  private final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  private final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry;

  private final Random random;

  // See Javadoc for #registerPendingResponse for more details.
  private final Map<Long, CompletableFuture<BtpResponse>> pendingResponses;

  private final StandardWebSocketClient wsClient;
  private WebSocketSession webSocketSession;

  /**
   * Required-args Constructor.
   */
  public BtpIlpClient(
    final BtpClientPluginSettings settings,
    final CodecContext ilpCodecContext,
    //final CodecContext btpCodecContext,
    final StandardWebSocketClient wsClient,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter
  ) {
    super(settings);

    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    //this.btpCodecContext = Objects.requireNonNull(btpCodecContext);

    this.binaryMessageToBtpPacketConverter = Objects.requireNonNull(binaryMessageToBtpPacketConverter);
    this.btpPacketToBinaryMessageConverter = Objects.requireNonNull(btpPacketToBinaryMessageConverter);

    this.wsClient = Objects.requireNonNull(wsClient);

    this.btpSubProtocolHandlerRegistry = new BtpSubProtocolHandlerRegistry();
    this.btpSubProtocolHandlerRegistry.putHandler(
      // Add BTP Subprotocol Handler for ILPv4
      BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP, new LoggingIlpBtpSubprotocolHandler(this.ilpCodecContext)
    );

    this.random = new SecureRandom();
    this.pendingResponses = Maps.newConcurrentMap();
  }

  /**
   * Perform the logic of actually connecting to the remote peer.
   */
  @Override
  public CompletableFuture<Void> doConnect() {

    try {
      // Connect and initialize the Session...
      this.webSocketSession =
        wsClient.doHandshake(
          new BinaryWebSocketHandler() {
            @Override
            protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
              // TODO: What does the other side of the Websocket see if there's an exception here?
              onIncomingBinaryMessage(session, message)
                .ifPresent(response -> {
                  try {
                    session.sendMessage(response);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                });
            }
          },
          "{scheme}://{localhost}:{port}/btp",
          getPluginSettings().getRemotePeerScheme(),
          getPluginSettings().getRemotePeerHostname(),
          getPluginSettings().getRemotePeerPort())
          .get();

      // AUTH
      final long requestId = nextRequestId();
      final String authToken = getPluginSettings().getSecret();
      final BtpMessage btpAuthMessage = this.constructAuthMessage(requestId, authToken);
      final BinaryMessage binaryAuthMessage = btpPacketToBinaryMessageConverter.convert(btpAuthMessage);
      LOGGER.debug(
        "Websocket Auth BinaryMessage Bytes: {}",
        BaseEncoding.base16().encode(binaryAuthMessage.getPayload().array())
      );
      this.sendMessage(binaryAuthMessage);
      return this.registerPendingResponse(btpAuthMessage.getRequestId())
        .thenAccept((response) -> {
        });
    } catch (InterruptedException | ExecutionException e) {
      return this.disconnect();
    }

  }

  /**
   * Perform the logic of disconnecting from the remote peer.
   */
  @Override
  public CompletableFuture<Void> doDisconnect() {
    // TODO: Disconnect! See Server code...
    throw new RuntimeException("FIXME");
  }

  /**
   * Perform the logic of sending a packet to a remote peer.
   *
   * @param preparePacket
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> doSendData(final InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException {

    Objects.requireNonNull(preparePacket, "preparePacket must not be null!");

    // Convert the ILP packet into a BTP message, and send it to the Websocket server...


    this.btpSubProtocolHandlerRegistry.getHandler(BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP)
      .map(handler -> (IlpBtpSubprotocolHandler) handler)
      .map(ilpHandler -> ilpHandler.sendData(preparePacket))
      .orElseThrow(() -> new RuntimeException("No BTP Subprotocol Handler registered for ILPv4!"));
  }

  /**
   * Implemented to support BTP.
   *
   * @param btpMessage
   *
   * @return
   */
  @Override
  protected CompletableFuture<BtpResponse> doSendDataOverBtp(BtpPacket btpMessage) throws BtpRuntimeException {
    return null;
  }

  /**
   * Perform the logic of settling with a remote peer.
   *
   * @param amount
   */
  @Override
  protected CompletableFuture<Void> doSendMoney(BigInteger amount) {
    throw new RuntimeException("FIXME!");
  }

  /**
   * Handle an incoming BinaryMessage from a Websocket by converting it into a {@link BtpMessage}and forwarding it to a
   * BTP processor.
   *
   * @param webSocketSession
   * @param incomingBinaryMessage
   *
   * @return A {@link BinaryMessage} that can immediately be returned to the caller (this response will contain
   * everything required to be eligible as a BTP response), or nothing if the response is {@link Optional#empty()}.
   */
  private Optional<BinaryMessage> onIncomingBinaryMessage(
    final WebSocketSession webSocketSession, final BinaryMessage incomingBinaryMessage
  ) {
    Objects.requireNonNull(webSocketSession);
    Objects.requireNonNull(incomingBinaryMessage);

    // TODO: Is the underlying map already synchronized?
    final BtpSession btpSession;
    synchronized (webSocketSession) {
      btpSession = (BtpSession) webSocketSession.getAttributes()
        .getOrDefault("btp-session", new BtpSession(this.getPluginSettings().getPeerAccountAddress()));
    }

    // If there's a problem deserializing the BtpPacket from the BinaryMessage, then close the connection and
    // return empty. This is one of the "tricky cases" as defined in the BTP spec where we don't want to get into
    // an infinite loop.
    final BtpPacket incomingBtpPacket;
    try {
      incomingBtpPacket = this.binaryMessageToBtpPacketConverter.convert(incomingBinaryMessage);
    } catch (BtpConversionException btpConversionException) {
      LOGGER.error("Unable to deserialize BtpPacket from incomingBinaryMessage: {}", btpConversionException);
      try {
        this.disconnect().get();
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
      return Optional.empty();
    }
  }

  /**
   * Returns the next random request id using a PRNG.
   */
  protected long nextRequestId() {
    return Math.abs(random.nextInt());
  }

  /**
   * Accessor the the BTP Plugin type of this BTP Plugin.
   */
  @Override
  public BtpPluginType getBtpPluginType() {
    return null;
  }


  protected void sendMessage(final WebSocketMessage webSocketMessage) {
    Objects.requireNonNull(webSocketMessage);

    try {
      // TODO: Check for "isConnected"?
      webSocketSession.sendMessage(webSocketMessage);
    } catch (IOException e) {
      try {
        this.disconnect().get();
      } catch (Exception e1) {
        throw new RuntimeException(e1);
      }
      throw new RuntimeException(e);
    }

  }

  /**
   * <p>Register and return a "pending response", mapping it to the supplied {@code requestId}. This mechanism works by
   * returning a completed future to a caller, who then waits for the future to be completed. The receiver processes the
   * request, and eventually returns a response by completing the appropriate <tt>pending respsonse</tt>.</p>
   *
   * <p>The following diagram illustrates this flow:</p>
   *
   * <pre>
   * ┌──────────┐                                              ┌──────────┐
   * │          │────────────Request (Object)─────────────────▷│          │
   * │          │                                              │          │
   * │          │             Reponse (Uncompleted             │          │
   * │          │◁─────────────CompletableFuture)───△──────────┤          │
   * │          │                                   │          │          │
   * │          │                                   │          │          │
   * │          │                                   │          │          │
   * │          │                                   │          │          │
   * │  Sender  │                                   │ Complete │ Receiver │
   * │          │                                   └or Timeout┤          │
   * │          │                                              │          │
   * │          │                                              │          │
   * │          │                                              │          │
   * │          │                                              │          │
   * │          │                                              │          │
   * │          │                                              │          │
   * │          │                                              │          │
   * └──────────┘                                              └──────────┘
   * </pre>
   *
   * @param requestId The unique identifier of the request that should receive a response, but only once that response
   *                  can be returned.
   *
   * @return
   */
  protected CompletableFuture<BtpResponse> registerPendingResponse(final long requestId) {

    final CompletableFuture<BtpResponse> pendingResponse = CompletableFuture.supplyAsync(
      () -> {
        // TODO: Configure this amount as a property.
        // TODO: Move back to seconds and set a default of 15.
        LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(15));
        throw new BtpRuntimeException(new RuntimeException("BTP SendData Timed-out!"));
      }
    );

    if (this.pendingResponses.putIfAbsent(requestId, pendingResponse) == null) {
      return pendingResponse;
    } else {
      // TODO: Just log an error and ignore?
      throw new RuntimeException("Encountered BTP message twice!");
    }
  }
}