package org.interledger.connector.link.events;

import org.immutables.value.Value;
import org.interledger.connector.link.Link;

/**
 * Emitted after a lpi2 connects to a remote peer.
 */
public interface LinkErrorEvent extends LinkEvent {

  static LinkErrorEvent of(final Link<?> link, final Throwable error) {
    return ImmutableLinkErrorEvent.builder().link(link).error(error).build();
  }

  /**
   * @return An error that the link emitted.
   */
  Throwable error();

  @Value.Immutable
  abstract class AbstractLinkErrorEvent implements LinkErrorEvent {

  }

}