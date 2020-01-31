package org.interledger.connector.pubsub;

/**
 * Proxies an object in order prevent potential feedback loops from messages received from the shared topic.
 */
public interface CoordinationProxyGenerator {

  /**
   * Create a proxy that forwards all method invocations to the original instance, but marks the instance
   * as having been received from the shared topic via an additional interface.
   * @param instance object to be proxied
   * @return proxied instance
   */
  Object createCoordinatedProxy(Object instance);
}
