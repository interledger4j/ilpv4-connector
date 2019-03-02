package com.sappenin.interledger.ilpv4.connector.links;

import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkHandler;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * <p>An extension of {@link AbstractLink} that provides base-line functionality for all incoming packets destined
 * for `peer.` addresses, which are always handled internally (i.e., handled by this Connector) and never forwarded to a
 * remote peer Connector.</p>
 *
 * <p>Examples of special addresses that should not be forwarded to a peer include <tt>`peer.config`</tt> and
 * any address starting with the <tt>`self`</tt> prefix.</p>
 *
 * <p>In the case of <tt>`peer.config`</tt>, the connector's routing-table is configured to forward all traffic for
 * such internally-routed destinations to a specifically configured link that extends this base class. In the case of
 * the peer-config protocol, packets will come into the connector with that destination address, and the routing table
 * will forward to the appropriate link that can handle `peer.config` messages per the CCP routing protocol.</p>
 *
 * <p>In this way, links that extend this class can function like a sub-network internal to this connector. Traffic
 * is still routed using Interledger, but it does not exit the connector like a normally-schemed packet would.</p>
 *
 * <pre>
 *                                                 ┌────────────┐                ┌────────────────┐
 *                ┌───────┐                        │ Connector  │                │   Internally   │
 * ──sendPacket──▷│ Link  │──handleIncomingPacket─▷│   Switch   │──switchPacket─▶│   Routed Link  │
 *                └───────┘                        └────────────┘                └────────────────┘
 * </pre>
 *
 * @deprecated This functionality will eventually be removed. Operations whose value accrue to the Connector should be
 * implemented in a PacketSwitchFilter (i.e., not switched) and the concept of "local accounts" should just be an
 * outgoing destination link that supports _many_ accounts (kind of like a mini-connector).
 */
@Deprecated
public abstract class InternallyRoutedLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final CodecContext ilpCodecContext;

  /**
   * Required-args constructor.
   *
   * @param linkSettings
   * @param ilpCodecContext
   */
  protected InternallyRoutedLink(final LinkSettings linkSettings, final CodecContext ilpCodecContext) {
    super(linkSettings);
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
  }

  @Override
  public CompletableFuture<Void> doConnect() {
    // No-op. Internally-routed links are always connected, so there is no connect/disconnect logic required.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> doDisconnect() {
    // No-op. Internally-routed links are always connected, so there is no connect/disconnect logic required.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void registerLinkHandler(LinkHandler ilpDataHandler) throws LinkHandlerAlreadyRegisteredException {
    throw new RuntimeException(
      "Internally-routed links never have incoming data, and thus should not have a registered DataHandler."
    );
  }

  /**
   * Accessor for the {@link CodecContext} of this link.
   */
  protected CodecContext getIlpCodecContext() {
    return ilpCodecContext;
  }
}
