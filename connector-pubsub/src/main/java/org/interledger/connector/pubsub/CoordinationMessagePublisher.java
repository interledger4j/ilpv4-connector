package org.interledger.connector.pubsub;

public interface CoordinationMessagePublisher {

    void publish(String message);

}