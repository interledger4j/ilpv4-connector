package com.sappenin.ilpv4.server.btp;

import com.sappenin.ilpv4.model.InterledgerAddress;
import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.model.PluginType;
import org.immutables.value.Value;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * <p>An extension of {@link Plugin} that is capable of representing a data channel with no money involved. It will
 * send BTP messages with no knowledge of the data within, so it can be used for ILP packets. The {@link
 * #settle(BigInteger)} function is a no-op because, by default, there is no system involved in handling money with
 * BTP.</p>
 *
 * <p>Two functions must be defined in order for this plugin to handle money. The first is
 * {@link #settle(BigInteger)}, which sends an amount of units to the peer for this plugin. This should be done via a
 * BTP <tt>TRANSFER</tt> call. The second method is {@link #onIncomingSettle(BigInteger)}, which is called on an
 * incoming BTP
 * <tt>TRANSFER</tt> message.</p>
 *
 * <p>The main use of this plugin, however, is as a building block for plugins that _do_ have an underlying ledger.</p>
 */
public class BtpPlugin implements Plugin {

  // While a plugin can handle multiple account addresses (one per session), it should only connect to a single node
  // address.
  private final InterledgerAddress peerAddress;
  private final CodecContext codecContext;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Required-args Constructor.
   */
  public BtpPlugin(
    final InterledgerAddress peerAddress, final CodecContext codecContext
    //, final BtpSubProtocolHandlerRegistry registry
  ) {
    this.codecContext = Objects.requireNonNull(codecContext);
    this.peerAddress = Objects.requireNonNull(peerAddress);

    //this.registry = Objects.requireNonNull(registry);
    // Add an ILP Sub-protocol handler here...
    //this.registry.putHandler(BTP_SUB_PROTOCOL_ILP, new IlpSubprotocolHandler(codecContext, this));
  }

  @Override
  public InterledgerAddress getPeerAddress() {
    return this.peerAddress;
  }

  @Override
  public void doConnect() {
    // This method will be called for all plugins. If this is a listenng-only variant (i.e., no external connection
    // needs to be made, then this should be a no-op. However, if a remote connection needs to be made, then we should
    // connect to the remove using a Websocket client.
    logger.error("Not yet implemented for Client variant!");

    // TODO: Make two different sub-classes (?), one for client-initiated and one for a server?

    // TODO: We want the server-variant to only require serve settings, and the client-variant to only require client
    // settings. Each of these will be configured at runtime, so perhaps we have two types of BTP connector.
  }

  @Override
  public void doDisconnect() {

  }

  @Override
  public CompletableFuture<InterledgerFulfillPacket> sendPacket(
    final InterledgerPreparePacket preparePacket
  ) throws InterledgerProtocolException {
    Objects.requireNonNull(preparePacket);

    // Get a BTP Session and send a message to it...

    // Use the underlying websocket stuff to send this packet...

    //    this.toBinaryMessage(
    //      BtpResponse.builder()
    //        .requestId(btpMessage.getRequestId())
    //        .subProtocols(responses)
    //        .build()
    //    );
    //    {
    //      type: BtpPacket.TYPE_MESSAGE,
    //        requestId: await _requestId(),
    //      data: { protocolData: [{
    //      protocolName: 'ilp',
    //        contentType: BtpPacket.MIME_APPLICATION_OCTET_STREAM,
    //        data: buffer
    //    }] }
    //    }

    return null;


  }

  /**
   * This method is only ever called by the {@link IlpSubprotocolHandler} as part of the BTP Websock machinery.
   */
  @Override
  public final CompletableFuture<InterledgerFulfillPacket> onIncomingPacket(final InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException {

    Objects.requireNonNull(preparePacket);

    if (logger.isDebugEnabled()) {
      logger.debug("Handling InterledgerPreparePacket from {}", this.getPeerAddress());
    }

    //    final Account sourceAccount = this.accountManager.getAccount(session.getAccountId())
    //      .orElseThrow(() -> new RuntimeException(
    //        String.format("No Account found for Interledger Address: %s", session.getAccountId()))
    //      );

    // If local...


    // else, forward...

    // TODO: Grab the proper plugin here from the routing table..
    Plugin plugin = null;

    // TODO: Adjust the packet, if needed (expiry, amount, etc?)
    return plugin.sendPacket(preparePacket);
  }

  @Override
  public void settle(BigInteger amount) {
    // No-op in vanilla BTP. Can be extended by an ILP Plugin.
  }


  @Override
  public void onIncomingSettle(BigInteger amount) {
    // No-op in vanilla BTP. Can be extended by an ILP Plugin.
  }

  @Override
  public PluginType getPluginType() {
    return PluginType.BTP;
  }

  //  /**
  //   * Convert a {@link BtpPacket} into a {@link BinaryMessage}.
  //   *
  //   * @param packet An instance of {@link BtpPacket}.
  //   *
  //   * @return An instance of {@link BinaryMessage}.
  //   *
  //   * @throws IOException
  //   */
  //  protected BinaryMessage toBinaryMessage(final BtpPacket packet) throws IOException {
  //    Objects.requireNonNull(packet);
  //    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
  //    codecContext.write(packet, baos);
  //    return new BinaryMessage(baos.toByteArray());
  //  }
  //
  //  private BtpMessage toBtpMessage(final BinaryMessage binaryMessage) throws IOException {
  //    final ByteBuffer buffer = Objects.requireNonNull(binaryMessage).getPayload();
  //    final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
  //    return codecContext.read(BtpMessage.class, stream);
  //  }
  //
  //
  //  /**
  //   * Store the username and token into this Websocket session.
  //   *
  //   * @param username The username of the signed-in account.
  //   * @param token    An authorization token used to authenticate the indicated user.
  //   */
  //  private void storeAuthInWebSocketSession(
  //    final WebSocketSession webSocketSession, final String username, final String token
  //  ) {
  //    Objects.requireNonNull(username);
  //    Objects.requireNonNull(token);
  //
  //    Objects.requireNonNull(webSocketSession).getAttributes().put(BtpSession.ACCOUNT_KEY, username + ":" + token);
  //  }
  //
  //  private boolean isAuthenticated(final WebSocketSession webSocketSession) {
  //    return Objects.requireNonNull(webSocketSession).getAttributes().containsKey(BtpSession.ACCOUNT_KEY);
  //  }

  /**
   * Indicates whether or not an instance of {@link BtpPlugin} is the client or the server in a websock session used for
   * BTP communications.
   */
  public enum BtpPluginType {

    /**
     * This plugin is operating as a websocket <tt>client</tt>, and must connect to a remote server in order to begin a
     * BTP session.
     */
    CLIENT,

    /**
     * This plugin is operating as the <tt>server</tt> in a websocket connection, waiting for remote peers to connect to
     * it in order to begin a BTP session
     */
    SERVER;
  }

  /**
   * Settings for a BTP Plugin when the plugin is operating as a Websocket server.
   */
  @Value.Immutable
  public interface BtpServerSettings {

    /**
     * The shared secret that a remote peer will use to authenticate to this connector using Websockets and the {@link
     * BtpSubProtocolHandlerRegistry#BTP_SUB_PROTOCOL_AUTH_TOKEN}.
     */
    String secret();
  }

  /**
   * Settings for a BTP Plugin when the plugin is operating as a Websocket server.
   */
  @Value.Immutable
  public interface BtpClientSettings {

    /**
     * <p>The remote websock address that this plugin will use to connect to its peer. For example,
     * <tt>btp+ws://:shh_its_a_secret@localhost:9000</tt></p>
     *
     * @return
     */
    String remotePeerUrl();
  }

}
