package org.interledger.connector.core.events;

/**
 * A parent interface for all Connector events.
 */
public interface ConnectorEvent<T> {

  /**
   * An optional message for this event.
   */
  String message();

  /**
   * Accessor for an arbitrary object inside of this event.
   */
  T object();

}
