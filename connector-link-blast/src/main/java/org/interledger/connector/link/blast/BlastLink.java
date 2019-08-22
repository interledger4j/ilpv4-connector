package org.interledger.connector.link.blast;

import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.link.PingableLink;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An {@link AbstractLink} that handles BLAST (aka, ILP over HTTP) connections.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
public class BlastLink extends AbstractLink<BlastLinkSettings> implements PingableLink<BlastLinkSettings> {

  public static final String LINK_TYPE_STRING = "BLAST";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  // Note: The RestTemplate in this sender is shared between all links...
  private BlastHttpSender blastHttpSender;

  /**
   * Required-args Constructor.
   *
   * @param blastLinkSettings A {@link BlastLinkSettings} that specified ledger link options.
   * @param linkEventEmitter  A {@link LinkEventEmitter} that is used to emit events from this link.
   * @param blastHttpSender   A {@link BlastHttpSender} used to send messages with the remote BLAST peer.
   * @param linkEventEmitter  A {@link LinkEventEmitter}.
   */
  public BlastLink(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final BlastLinkSettings blastLinkSettings,
    final BlastHttpSender blastHttpSender,
    final LinkEventEmitter linkEventEmitter
  ) {
    super(operatorAddressSupplier, blastLinkSettings, linkEventEmitter);
    this.blastHttpSender = Objects.requireNonNull(blastHttpSender);
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
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    // While this call to blastHttpSender appears to block, the @Async annotation in this method actually instructs
    // Spring to wrap the entire method in a Proxy that runs in a separate thread. Thus, this return is simply to
    // conform to the Java API.
    return blastHttpSender.sendData(preparePacket);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BlastLink blastLink = (BlastLink) o;

    return blastHttpSender.equals(blastLink.blastHttpSender);
  }

  @Override
  public int hashCode() {
    return blastHttpSender.hashCode();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BlastLink.class.getSimpleName() + "[", "]")
      .add("blastHttpSender=" + blastHttpSender)
      .toString();
  }
}
