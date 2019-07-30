package org.interledger.ilpv4.connector.it.topology;

/**
 * A node in a topology, which wraps a single object of a particular type. For example, a node might contain an ILP
 * Connector running in a Spring Boot runtime, a Redis Server, or any other runtime.
 */
public interface Node<T> {

  String getId();

  /**
   * Start this node. Sometimes a node is started when the topology starts-up, but not always (e.g., a delayed start).
   */
  void start();

  /**
   * Stop this node.
   */
  void stop();

  /**
   * Accessor for the {@link T} contained in this Node.
   *
   * @return An instance of {@link T}.
   */
  T getContentObject();
}
