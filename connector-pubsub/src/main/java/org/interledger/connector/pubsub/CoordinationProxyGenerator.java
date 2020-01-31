package org.interledger.connector.pubsub;

public interface CoordinationProxyGenerator {

  Object createCoordinatedProxy(Object instance);
}
