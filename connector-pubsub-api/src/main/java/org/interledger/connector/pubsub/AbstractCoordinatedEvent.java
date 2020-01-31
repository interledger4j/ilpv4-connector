package org.interledger.connector.pubsub;

import java.util.Objects;

/**
 * Base class for events that require coordination across a cluster of connectors.
 */
public abstract class AbstractCoordinatedEvent {

  private boolean receivedViaCoordination = false;

  /**
   * Indicates that a message was received from the shared topic
   * @return true if the message came from the shared topic
   */
  final boolean receivedViaCoordination() {
    return receivedViaCoordination;
  }

  /**
   * Modifies the instance to indicate it was received via the shared topic
   */
  final void markReceivedViaCoordination() {
    this.receivedViaCoordination = true;
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
    return receivedViaCoordination == that.receivedViaCoordination;
  }

  @Override
  public int hashCode() {
    return Objects.hash(receivedViaCoordination);
  }
}
