package com.sappenin.ilpv4.client;

/**
 * An {@link IlpClient} that extends {@link AbstractIlpClient} for connecting to an ILSP using Websockets.
 */
public class BtpIlpClient_DELETE { //extends AbstractIlpClient<IlpClientSettings> implements IlpClient<IlpClientSettings> {
//    private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//    private final CodecContext ilpCodecContext;
//    private final CodecContext btpCodecContext;
//    private final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry;
//
//    // See Javadoc for #registerPendingResponse for more details.
//    private final Map<Long, CompletableFuture<BtpResponse>> pendingResponses;
//
//    private final StandardWebSocketClient wsClient;
//
//    /**
//     * Required-args Constructor.
//     *
//     * @param ilpClientSettings A {@link IlpClientSettings} that specified ledger plugin options.
//     * @param ilpCodecContext
//     * @param btpCodecContext
//     * @param wsClient          A {@link WebSocketClient} used to connect to an ILSP's Websocket server.
//     */
//    protected BtpIlpClient_DELETE(
//      final IlpClientSettings ilpClientSettings, final CodecContext ilpCodecContext, final CodecContext btpCodecContext,
//      final StandardWebSocketClient wsClient
//    ) {
//      super(ilpClientSettings);
//      this.pendingResponses = Maps.newConcurrentMap();
//
//      this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
//      this.btpCodecContext = Objects.requireNonNull(btpCodecContext);
//
//      this.wsClient = Objects.requireNonNull(wsClient);
//
//      this.btpSubProtocolHandlerRegistry = new BtpSubProtocolHandlerRegistry();
//      this.btpSubProtocolHandlerRegistry.putHandler(
//        // Add BTP Subprotocol Handler for ILPv4
//        BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP, new LoggingIlpBtpSubprotocolHandler(this.ilpCodecContext)
//      );
//
//      // When ILP data comes into this client, handle it using an instance of LoggingBtpSubProtocolHandler.
//      this.registerDataHandler((sourceAccountAddress, sourcePreparePacket) -> {
//        throw new RuntimeException("ILPv4 BTP Clients cannot process incoming ILP Prepare packets!");
//      });
//
//      // When ILP data comes into this client, handle it using an instance of LoggingBtpSubProtocolHandler.
//      this.registerMoneyHandler((amount) -> logger.info("Connector settled for {} units!", amount));
//
//    }
//
//    /**
//     * Perform the logic of actually connecting to the remote peer.
//     */
//    @Override
//    public CompletableFuture<Void> doConnect() {
//      return null;
//    }
//
//    /**
//     * Perform the logic of disconnecting from the remote peer.
//     */
//    @Override
//    public CompletableFuture<Void> doDisconnect() {
//      return null;
//    }
//
//    /**
//     * Perform the logic of sending a packet to a remote peer.
//     *
//     * @param preparePacket
//     */
//    @Override
//    public CompletableFuture<InterledgerFulfillPacket> doSendData(final InterledgerPreparePacket preparePacket)
//      throws InterledgerProtocolException {
//
//      Objects.requireNonNull(preparePacket, "preparePacket must not be null!");
//
//      // Convert the ILP packet into a BTP message, and send it to the Websocket server...
//
//    }
//
//    //  /**
//    //   * Send data using the BTP connection over the {@link #wsClient}. This method is typically called by the plugin in
//    //   * response to {@link #sendData(InterledgerPreparePacket)}.
//    //   *
//    //   * @param btpPacket
//    //   *
//    //   * @return
//    //   */
//    //  @Override
//    //  protected CompletableFuture<BtpResponse> doSendDataOverBtp(final BtpPacket btpPacket) {
//    //    Objects.requireNonNull(btpPacket);
//    //
//    //    // Send the BtpMessage to the remote peer using the websocket client....but first translate the BtpMessage to a
//    //    // binary message.
//    //    try {
//    //      final BinaryMessage binaryMessage = this.btpPacketToBinaryMessageConverter.convert(btpPacket);
//    //
//    //      // Send the message out. A response will come back as a pending request (see below).
//    //      sendMessage(binaryMessage);
//    //
//    //      //transferQueue.poll(5, TimeUnit.MINUTES);
//    //
//    //      //Use https://github.com/sarveswaran-m/blockingMap4j
//    //      //      boolean response =
//    //      //        this.transferQueue.removeIf(btpResponse -> btpResponse.getRequestId() == btpMessage.getRequestId());
//    //
//    //      return registerPendingResponse(btpPacket.getRequestId());
//    //    } catch (Exception e) {
//    //      throw new RuntimeException(e);
//    //    }
//    //  }
//
//    /**
//     * Perform the logic of settling with a remote peer.
//     *
//     * @param amount
//     */
//    @Override
//    protected CompletableFuture<Void> doSendMoney(BigInteger amount) {
//      return null;
//    }
//
//    protected void sendMessage(final WebSocketMessage webSocketMessage) {
//      Objects.requireNonNull(webSocketMessage);
//
//        try {
//          // TODO: Check for "isConnected"?
//          wsClient.sendMessage(webSocketMessage);
//        } catch (IOException e) {
//          try {
//            this.disconnect().get();
//          } catch (Exception e1) {
//            throw new RuntimeException(e1);
//          }
//          throw new RuntimeException(e);
//        }
//      });
//    }
//
//    /**
//     * <p>Register and return a "pending response", mapping it to the supplied {@code requestId}. This mechanism works by
//     * returning a completed future to a caller, who then waits for the future to be completed. The receiver processes the
//     * request, and eventually returns a response by completing the appropriate <tt>pending respsonse</tt>.</p>
//     *
//     * <p>The following diagram illustrates this flow:</p>
//     *
//     * <pre>
//     * ┌──────────┐                                            ┌──────────┐
//     * │          │────────────Request (Object)───────────────▷│          │
//     * │          │                                            │          │
//     * │          │            Reponse (Uncompleted            │          │
//     * │          │◁────────────CompletableFuture◁──────┬──────│          │
//     * │          │                                     │      │          │
//     * │          │                                     │      │          │
//     * │          │                                     │      │          │
//     * │          │                                     │      │          │
//     * │  Sender  │                                 Complete   │ Receiver │
//     * │          │                                or Timeout  │          │
//     * │          │                                     │      │          │
//     * │          │                                     │      │          │
//     * │          │                                     │      │          │
//     * │          │                                     │      │          │
//     * │          │                                     │      │          │
//     * │          │                                     └──────┤          │
//     * │          │                                            │          │
//     * └──────────┘                                            └──────────┘
//     * </pre>
//     *
//     * @param requestId The unique identifier of the request that should receive a response, but only once that response
//     *                  can be returned.
//     *
//     * @return
//     */
//    protected CompletableFuture<BtpResponse> registerPendingResponse(final long requestId) {
//
//      final CompletableFuture<BtpResponse> pendingResponse = CompletableFuture.supplyAsync(
//        () -> {
//          // TODO: Configure this amount as a property.
//          // TODO: Move back to seconds and set a default of 15.
//          LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(15));
//          throw new BtpRuntimeException(new RuntimeException("BTP SendData Timed-out!"));
//        }
//      );
//
//      if (this.pendingResponses.putIfAbsent(requestId, pendingResponse) == null) {
//        return pendingResponse;
//      } else {
//        // TODO: Just log an error and ignore?
//        throw new RuntimeException("Encountered BTP message twice!");
//      }
//    }


}
