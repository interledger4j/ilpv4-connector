package org.interledger.connector.link.events;

import org.immutables.value.Value;
import org.interledger.connector.link.Link;

/**
 * Emitted after a lpi2 connects to a remote peer.
 */
public interface LinkConnectedEvent extends LinkEvent {

  static LinkConnectedEvent of(final Link<?> link) {
    return ImmutableLinkConnectedEvent.builder().link(link).build();
  }

  @Value.Immutable
  abstract class AbstractLinkConnectedEvent implements
    LinkConnectedEvent {

  }

}