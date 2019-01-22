package com.sappenin.interledger.ilpv4.connector.plugins.connectivity;

import org.interledger.plugin.lpiv2.Plugin;

/**
 * <p>A LPIv2 {@link Plugin} that implements the <tt>echo</tt> ILP Sub-protocol, which merely echoes back a fulfillment
 * to the caller in order to simulate something like <tt>ping</tt>.</p>
 *
 * <p>This controller is only ever engaged if a packet is addressed to _this_ Connector, so we don't actually need
 * to route this packet anywhere. Ping-protocol requests that are addressed to other nodes would not enter this
 * controller, and instead would be forwarded to the correct destination, if possible.</p>
 *
 * @see "https://github.com/interledger/rfcs/pull/232"
 * @deprecated This class is currently deprecated and will be removed because, in its current form, the echo protocol
 * enables DDOS attacks at the ILP layer because any node can assemble an echo-packet with an arbitrary echo-destination
 * address.
 *
 * Some problems: 1.) The protocol uses to payments to indicate connectivity. However, this is not always the case. For
 * example, a receiver may not be able to actually send 0-value payments. Or, an intermediary might not as well.
 * Instead, it would be better to have a "ping" that is a 0-value payment with any condition. The "pong" can basically
 * be an ILP fulfill (0-value) or a reject (doesn't matter which) with some ILP data in it that contains anything
 * necessary for the payload. 2.) the protocol uses a wonky destination address called "ECHO" in the ILP packet. This is
 * to allow this packet to be forwarded to any participant in a payment chain. Instead of relying upon this special
 * internal-prefix, we should create a first-class allocation scheme that allows this to be used. We obviously have the
 * need to be able to send non-ILP messages to other ILP nodes in the network (without involving a payment).
 * Counter-point is that these "ping" request _should_ involve a very small amount of money, in which case we need some
 * other indicator in the ILP sub-protocol data, but probably should not be "ECHO".
 */
@Deprecated
public class EchoProtocolPlugin { //extends AbstractPlugin<PluginSettings> implements Plugin<PluginSettings> {

  //  public static final String PLUGIN_TYPE_STRING = "ECHO_PLUGIN";
  //  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);
  //
  //  static final String ECHO_DATA_PREFIX = "ECHOECHOECHOECHO";
  //
  //  private static final int MINIMUM_ECHO_PACKET_DATA_LENGTH = 16 + 1;
  //  private static final int ECHO_DATA_PREFIX_LENGTH = ECHO_DATA_PREFIX.length();
  //
  //  // When an outgoing `ping` payment is sent "out", the condition is stored into this map. Later, an incoming `pong`
  //  // payment will be received that _should_ contain the original condition. This Map stores that relationship.
  //  private final Map<InterledgerCondition, InterledgerFulfillment> pingWithEchoConditionMap;
  //
  //  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  //
  //  private final CodecContext oerCodecContext;
  //  private final EchoPacketService echoPacketService;
  //
  //  /**
  //   * Required-args Constructor.
  //   *
  //   * @param pluginSettings    A {@link PluginSettings} that specifies all plugin options.
  //   * @param oerCodecContext
  //   * @param echoPacketService
  //   */
  //  public EchoProtocolPlugin(
  //    final PluginSettings pluginSettings, final CodecContext oerCodecContext, final EchoPacketService echoPacketService
  //  ) {
  //    super(pluginSettings);
  //    this.oerCodecContext = Objects.requireNonNull(oerCodecContext);
  //    this.echoPacketService = Objects.requireNonNull(echoPacketService);
  //    this.pingWithEchoConditionMap = Maps.newConcurrentMap();
  //
  //    this.registerDataHandler((sourceAddress, incomingPacket) -> this.handleIncomingData(sourceAddress, incomingPacket));
  //  }
  //
  //  @Override
  //  public CompletableFuture<Void> doConnect() {
  //    // No-op.
  //    return CompletableFuture.completedFuture(null);
  //  }
  //
  //  /**
  //   * Perform the logic of disconnecting from the remote peer.
  //   */
  //  @Override
  //  public CompletableFuture<Void> doDisconnect() {
  //    // No-op.
  //    return CompletableFuture.completedFuture(null);
  //  }
  //
  //  /**
  //   * Ping protocol sends a prepare packet to a remote node.
  //   *
  //   * @param destinationAddress
  //   */
  //  public void pingWithEcho(final InterledgerAddress destinationAddress) {
  //    Objects.requireNonNull(destinationAddress);
  //    final InterledgerFulfillment randomFulfillment = this.echoPacketService.randomFulfillment();
  //    this.pingWithEchoConditionMap.put(randomFulfillment.getCondition(), randomFulfillment);
  //
  //    final InterledgerPreparePacket echoPacket = this.echoPacketService.constructEchoRequestPacket(
  //      this.getPluginSettings().getLocalNodeAddress(), // The client of the ping...
  //      destinationAddress, // The destination address to ping (and expect an echo from)
  //      randomFulfillment.getCondition()
  //    );
  //
  //    return null;
  //    //    this.routeData(echoPacket)
  //    //      .thenApply((response) -> {
  //    //
  //    //        //
  //    //
  //    //
  //    //      });
  //    //
  //  }
  //
  //  /**
  //   * Perform the logic of sending a type-0 <tt>echo</tt> packet to a remote peer.
  //   *
  //   * @param preparePacket An {@link InterledgerPreparePacket} to send to the remote peer.l
  //   */
  //  @Override
  //  public CompletableFuture<Optional<InterledgerResponsePacket>> doSendData(InterledgerPreparePacket preparePacket) {
  //
  //    final InterledgerFulfillment randomFulfillment = this.echoPacketService.randomFulfillment();
  //    this.pingWithEchoConditionMap.put(randomFulfillment.getCondition(), randomFulfillment);
  //
  //    // echoPacket = this.echoPacketService.constructEchoRequestPacket()
  //
  //    return null;
  //    //return CompletableFuture.supplyAsync(() -> throw new RuntimeException("Echo Plugin should never se"))
  //
  //  }
  //
  //  /**
  //   * Echo payments are always 0-value, so settlement over sendMoney is _never_ required.
  //   */
  //  @Override
  //  protected CompletableFuture<Void> doSendMoney(BigInteger amount) {
  //    // No-op.
  //    return CompletableFuture.completedFuture(null);
  //
  //  }
  //
  //  /**
  //   * Handles an incoming ILP fulfill packet.
  //   *
  //   * @param sourceAccountAddress
  //   * @param sourcePreparePacket
  //   *
  //   * @return A {@link InterledgerPreparePacket} that can be sent back to the sender of the original ping packet.
  //   */
  //  public CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingData(
  //    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  //  ) throws InterledgerProtocolException {
  //
  //    logger.debug("Handling Incoming IlpPreparePacket from `{}` for Echo: {}",
  //      sourceAccountAddress, sourcePreparePacket);
  //
  //    final int dataLength = sourcePreparePacket.getData().length;
  //
  //    if (dataLength < MINIMUM_ECHO_PACKET_DATA_LENGTH) {
  //      throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
  //        .code(F01_INVALID_PACKET)
  //        .triggeredBy(sourceAccountAddress)
  //        .message("Incoming IlpPacket data too short for echo request! length=" + dataLength)
  //        .build());
  //    }
  //
  //    // Doesn't copy bytes...
  //    final ByteBuffer prefixByteBuffer = ByteBuffer.wrap(sourcePreparePacket.getData(), 0, 16);
  //    // Copies bytes...
  //    //final byte[] prefixBytes = Arrays.copyOfRange(sourcePreparePacket.getData(), 0, 16);
  //    if (!prefixByteBuffer.toString().equals(ECHO_DATA_PREFIX)) {
  //      throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
  //        .code(F01_INVALID_PACKET)
  //        .triggeredBy(sourceAccountAddress)
  //        .message("Incoming IlpPacket data does not start with ECHO prefix!")
  //        .build());
  //    }
  //
  //    final ByteBuffer echoPayload = ByteBuffer.wrap(sourcePreparePacket.getData(), ECHO_DATA_PREFIX_LENGTH,
  //      dataLength - ECHO_DATA_PREFIX_LENGTH);
  //
  //    try {
  //      final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(echoPayload.array());
  //      short echoProtocolType = oerCodecContext.read(Short.class, byteArrayInputStream);
  //
  //      if (echoProtocolType == 0) {
  //
  //        final InterledgerAddress sourceAddressFromPacket =
  //          oerCodecContext.read(InterledgerAddress.class, byteArrayInputStream);
  //
  //        logger.debug("Responding to ILP ping. sourceAccount={} sourceAddress={} cond={]",
  //          sourceAccountAddress, sourceAddressFromPacket,
  //          Base64.getEncoder().encodeToString(sourcePreparePacket.getExecutionCondition().getHash())
  //        );
  //
  //        // Send an Echo payment back to the caller containing the "Pong Payment"...
  //        //        final UUID pingId = UUID.randomUUID();
  //        //        final String pingData =
  //        //          String.format("%s\n%s\n%s", ECHO_DATA_PREFIX, pingId, sourceAccountAddress.getValue());
  //
  //        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(18);
  //        //byteArrayOutputStream.write(ECHO_DATA_PREFIX.getBytes(US_ASCII));
  //        oerCodecContext.write(ECHO_DATA_PREFIX, byteArrayOutputStream);
  //        oerCodecContext.write((short) 00, byteArrayOutputStream);
  //
  //        //final String pingData = String.format("%s\n%s%s", ECHO_DATA_PREFIX, 00, 00);
  //
  //        // TODO: Extract echo functionality into its own module or package or project, perhaps.
  //        final InterledgerPreparePacket echoPacket = InterledgerPreparePacket.builder()
  //          .destination(sourceAccountAddress)
  //          .expiresAt(Instant.now().plusSeconds(120))
  //          .executionCondition(InterledgerCondition.of(new byte[32]))
  //          .data(byteArrayOutputStream.toByteArray())
  //          .build();
  //
  //        return null;
  //        //        return echoPacket;
  //      } else {
  //        throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
  //          .code(F01_INVALID_PACKET)
  //          .triggeredBy(sourceAccountAddress)
  //          .message("Incoming IlpPacket contained unexpected ping request!")
  //          .build());
  //      }
  //    } catch (IOException e) {
  //      logger.error("Received unexpected ping request! sourceAccount={} ilpPacket: {}", sourceAccountAddress,
  //        sourcePreparePacket);
  //      throw new InterledgerProtocolException(InterledgerRejectPacket.builder()
  //        .code(F01_INVALID_PACKET)
  //        .triggeredBy(sourceAccountAddress)
  //        .message("Incoming IlpPacket data does not have proper ECHO payload!")
  //        .build());
  //    }
  //  }
  //
  //    /**
  //     * Possible result-states for the echo protocol.
  //     */
  //    enum EchoResult {
  //
  //      CONNECTED,
  //      DISCONNECTED,
  //      TIMED_OUT
  //
  //    }
  //
  //    @Value.Default
  //    interface PingWithEchoResult {
  //
  //      /**
  //       * @return {@code CONNECTED} if this node can send value to the remote node, {@code DISCONNECTED} if this node is
  //       * not able to send data to the remote node; {@code TIMED_OUT} if the request did not complete in time.
  //       */
  //      EchoResult canSendData();
  //
  //      /**
  //       * @return {@code CONNECTED} if the remote node can send value to this node, {@code DISCONNECTED} if the remote node
  //       * is not able to send data to this node; {@code TIMED_OUT} if the remote request did not complete in time.
  //       */
  //      EchoResult canReceiveData();
  //    }
}
