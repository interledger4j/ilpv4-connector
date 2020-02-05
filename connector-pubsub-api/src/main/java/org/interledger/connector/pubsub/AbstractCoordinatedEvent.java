package org.interledger.connector.pubsub;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for events that require coordination across a cluster of connectors.
 */
public abstract class AbstractCoordinatedEvent {

  private AtomicBoolean receivedViaCoordination = new AtomicBoolean(false);

  /**
   * Indicates that a message was received from the shared topic
   * @return true if the message came from the shared topic
   */
  final boolean receivedViaCoordination() {
    return receivedViaCoordination.get();
  }

  /**
   * Modifies the instance to indicate it was received via the shared topic
   */
  final void markReceivedViaCoordination() {
    this.receivedViaCoordination.set(true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractCoordinatedEvent that = (AbstractCoordinatedEvent) o;
    return receivedViaCoordination.get() == that.receivedViaCoordination.get();
  }

  @Override
  public int hashCode() {
    return Objects.hash(receivedViaCoordination.get());
  }
}
