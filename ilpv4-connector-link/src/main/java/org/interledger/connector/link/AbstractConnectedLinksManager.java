package org.interledger.connector.link;

import com.google.common.collect.Maps;
import org.interledger.connector.link.events.LinkDisconnectedEvent;
import org.interledger.connector.link.events.LinkEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * An abstract implementation of {@link ConnectedLinksManager} that holds onto a link as long as its connected, and
 * removes the link upon a disconnect.
 *
 * @deprecated Consider Link tracking in more detail. There's an argument that we don't need to track Link connections,
 * instead just rejecting if a send fails. In that world, this manager could go away.
 */
@Deprecated
public abstract class AbstractConnectedLinksManager implements ConnectedLinksManager, LinkEventListener {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Map<LinkId, Link<?>> connectedLinks;

  public AbstractConnectedLinksManager() {
    this.connectedLinks = Maps.newConcurrentMap();
  }

  @Override
  public Link<?> putConnectedLink(final Link<?> link) {
    link.addLinkEventListener(this);
    return this.connectedLinks.putIfAbsent(link.getLinkId().get(), link);
  }

  @Override
  public Optional<Link<?>> getConnectedLink(final LinkId linkId) {
    Objects.requireNonNull(linkId);
    return Optional.ofNullable(connectedLinks.get(linkId));
  }

  public Stream<LinkId> getAllConnectedLinkIds() {
    return this.connectedLinks.keySet().stream();
  }

  @Override
  public CompletableFuture<Void> removeConnectedLink(final LinkId linkId) {
    Objects.requireNonNull(linkId);

    return this.getConnectedLink(linkId)
      .map(link -> link.disconnect().thenAccept(($) -> connectedLinks.remove(linkId)))
      .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  @Override
  public void onDisconnect(final LinkDisconnectedEvent event) {
    Objects.requireNonNull(event);
    this.removeConnectedLink(event.getLink().getLinkId().get());
  }
}
