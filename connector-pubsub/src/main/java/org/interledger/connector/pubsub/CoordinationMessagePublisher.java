package org.interledger.connector.pubsub;

/**
 * Publishes messages to a shared topic for distribution across the cluster
 */
public interface CoordinationMessagePublisher {

    void publish(AbstractCoordinatedEvent message);

}