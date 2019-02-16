package org.interledger.connector.link.events;

import org.immutables.value.Value;

/**
 * Emitted after a lpi2 connects to a remote peer.
 */
public interface LinkErrorEvent extends LinkEvent {

  /**
   * @return An error that the link emitted.
   */
  Exception getError();

  @Value.Immutable
  abstract class AbstractLinkErrorEvent implements LinkErrorEvent {

  }

}