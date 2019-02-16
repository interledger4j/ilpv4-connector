package org.interledger.connector.link.blast;

import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.link.blast.BlastHttpSender;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * An {@link AbstractLink} that handles BLAST (aka, ILP over HTTP) connections.
 *
 * @see "https://github.com/interledger/rfcs/TODO"
 */
public class BlastLink extends AbstractLink<BlastLinkSettings> {

  public static final String LINK_TYPE_STRING = "BlastLink";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  // This RestTemplate is shared between all links...
  private BlastHttpSender blastHttpSender;

  /**
   * Required-args Constructor. Utilizes a default {@link LinkEventEmitter} that synchronously connects to any event
   * handlers.
   *
   * @param linkSettings A {@link BlastLinkSettings} that specified ledger link options.
   * @param restTemplate A {@link RestTemplate} used to communicate with the remote BLAST peer.
   */
  public BlastLink(final BlastLinkSettings linkSettings, final RestTemplate restTemplate) {
    super(linkSettings);
    this.blastHttpSender = new BlastHttpSender(
      linkSettings.getOperatorAddress(),
      linkSettings.getOutgoingUrl().uri(),
      restTemplate,
      () -> linkSettings.getOutgoingSecret().getBytes()
    );
  }

  /**
   * Required-args Constructor.
   *
   * @param linkSettings     A {@link BlastLinkSettings} that specified ledger link options.
   * @param linkEventEmitter A {@link LinkEventEmitter} that is used to emit events from this link.
   */
  public BlastLink(
    final BlastLinkSettings linkSettings,
    final RestTemplate restTemplate,
    final LinkEventEmitter linkEventEmitter
  ) {
    super(linkSettings, linkEventEmitter);
    this.blastHttpSender = new BlastHttpSender(
      linkSettings.getOperatorAddress(),
      linkSettings.getOutgoingUrl().uri(),
      restTemplate,
      () -> linkSettings.getOutgoingSecret().getBytes());
  }

  /**
   * Reconfigure this link with a new {@link BlastLinkSettings}.
   *
   * @param blastLinkSettings
   */
  public void reconfigure(final BlastLinkSettings blastLinkSettings) {
    this.blastHttpSender = new BlastHttpSender(
      blastLinkSettings.getOperatorAddress(),
      blastLinkSettings.getOutgoingUrl().uri(),
      blastHttpSender.getRestTemplate(),
      () -> blastLinkSettings.getOutgoingSecret().getBytes()
    );
  }

  /**
   * Perform the logic of actually connecting to the remote peer.
   */
  @Override
  public CompletableFuture<Void> doConnect() {
    // No-op.
    return CompletableFuture.supplyAsync(() -> {
      // If the peer is not up, the server operating this link will warn, but will not fail. One side needs to
      // startup first, so it's likely that this test will fail for the first side to startup, but can be useful for
      // connection debugging.
      blastHttpSender.testConnection();
      return null;
    });
  }

  /**
   * Perform the logic of disconnecting from the remote peer.
   */
  @Override
  public CompletableFuture<Void> doDisconnect() {
    // No-op.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Optional<InterledgerResponsePacket> sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    // While this call to blastHttpSender appears to block, the @Async annotation in this method actually instructs
    // Spring to wrap the entire method in a Proxy that runs in a separate thread. Thus, this return is simply to
    // conform to the Java API.
    return Optional.ofNullable(blastHttpSender.sendData(preparePacket));
  }
}
